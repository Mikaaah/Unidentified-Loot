"""Publish the explicitly approved TNK artifact once, without rebuilding it."""
from pathlib import Path
import hashlib, io, json, os, sys, tomllib, urllib.error, urllib.request, uuid, zipfile

ROOT = Path(__file__).resolve().parents[2]
FOLDER = ROOT / '.github/publish/1.1.3'
CF = 'https://minecraft.curseforge.com'
RECEIPT = ROOT / 'curseforge-upload-receipt.json'
TOKEN = os.environ.get('CURSEFORGE_TOKEN', '')
state = {'project_id': 1753149, 'version': '1.1.3', 'status': 'validating'}
def save():
    RECEIPT.write_text(json.dumps(state, indent=2) + '\n')
def request_json(url, *, authenticated=False, body=None, content_type=None):
    headers = {'Accept': 'application/json', 'User-Agent': 'TNK-Unidentified-Loot-release/1.1.3'}
    if authenticated:
        headers['X-Api-Token'] = TOKEN
    if content_type:
        headers['Content-Type'] = content_type
    req = urllib.request.Request(url, data=body, headers=headers, method='POST' if body is not None else 'GET')
    with urllib.request.urlopen(req, timeout=90) as response:
        return json.load(response)

def version_id(versions, name):
    matches = [v for v in versions if v.get('name', '').casefold() == name.casefold()]
    ids = {int(v['id']) for v in matches}
    if len(ids) > 1:
        native = [v for v in matches if 'apiVersion' in v and v['apiVersion'] is None]
        if len({int(v['id']) for v in native}) == 1:
            return int(native[0]['id'])
    if len(ids) != 1:
        detail = [{k: v.get(k) for k in ('id', 'name', 'apiVersion', 'gameVersionTypeID')} for v in matches]
        raise RuntimeError('CurseForge version is missing or ambiguous: ' + name + '; candidates=' + json.dumps(detail))
    return ids.pop()

def main():
    if os.environ.get('GITHUB_REPOSITORY') != 'Mikaaah/Unidentified-Loot' or os.environ.get('GITHUB_REF') != 'refs/heads/publish/curseforge-1.1.3':
        raise RuntimeError('Publishing is restricted to the explicitly authorized release branch')
    if not TOKEN:
        raise RuntimeError('CURSEFORGE_TOKEN repository secret is missing')
    spec = json.loads((FOLDER / 'request.json').read_text())
    if spec['project_id'] != 1753149 or spec['version'] != '1.1.3':
        raise RuntimeError('Unexpected target project or version')
    jar = FOLDER / spec['filename']
    if jar.name != 'tnkloot-1.1.3-tnk-neoforge-1.21.1.jar':
        raise RuntimeError('Unexpected artifact filename')
    payload = jar.read_bytes()
    digest = hashlib.sha256(payload).hexdigest()
    if digest != spec['sha256']:
        raise RuntimeError('Approved artifact checksum mismatch')
    with zipfile.ZipFile(io.BytesIO(payload)) as z:
        meta = tomllib.loads(z.read('META-INF/neoforge.mods.toml').decode())
        if meta['mods'][0]['modId'] != 'tnkloot' or meta['mods'][0]['version'] != '1.1.3':
            raise RuntimeError('FML release identity mismatch')
        progression = next(d for d in meta['dependencies']['tnkloot'] if d['modId'] == 'tnkprogression')
        if progression['versionRange'] != '[1.1.0,)':
            raise RuntimeError('Progression dependency mismatch')
        if any('ClientVisualCheck' in n for n in z.namelist()):
            raise RuntimeError('Verification harness must not be published')
    state.update(filename=jar.name, sha256=digest, bytes=len(payload))
    save()
    existing = request_json('https://www.curseforge.com/api/v1/mods/1753149/files?pageSize=50&index=0')
    rows = existing.get('data', existing) if isinstance(existing, dict) else existing
    for row in rows:
        if row.get('fileName') == jar.name:
            state.update(status='already_uploaded', file_id=int(row['id']))
            save()
            print('CURSEFORGE_ALREADY_UPLOADED', state['file_id'], flush=True)
            return
    versions = request_json(CF + '/api/game/versions', authenticated=True)
    if isinstance(versions, dict):
        versions = versions.get('data', [])
    selected = []
    for label in spec['game_versions']:
        selected.append(version_id(versions, label))
    metadata = {'displayName': jar.name, 'releaseType': spec['release_type'],
                'changelogType': 'html', 'changelog': (FOLDER / 'changelog.html').read_text(),
                'gameVersions': selected, 'isMarkedForManualRelease': False,
                'relations': {'projects': spec['relations']}}
    state.update(game_versions=dict(zip(spec['game_versions'], selected)), status='ready')
    save()
    boundary = 'tnk-approved-' + uuid.uuid4().hex
    body = (f'--{boundary}\r\nContent-Disposition: form-data; name="metadata"\r\nContent-Type: application/json\r\n\r\n'.encode()
            + json.dumps(metadata).encode()
            + f'\r\n--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{jar.name}"\r\nContent-Type: application/java-archive\r\n\r\n'.encode()
            + payload + f'\r\n--{boundary}--\r\n'.encode())
    state['status'] = 'uploading'
    save()
    result = request_json(CF + '/api/projects/1753149/upload-file', authenticated=True,
                          body=body, content_type='multipart/form-data; boundary=' + boundary)
    file_id = int(result['id'])
    state.update(status='uploaded', file_id=file_id,
                 url='https://www.curseforge.com/minecraft/mc-mods/unidentified-loot-a-towns-kingdoms-mod/files/' + str(file_id))
    save()
    print('CURSEFORGE_UPLOAD_OK', file_id, digest, flush=True)
    print(state['url'], flush=True)

try:
    main()
except urllib.error.HTTPError as exc:
    state.update(status='rejected' if 400 <= exc.code < 500 else 'uncertain', http_status=exc.code)
    save()
    detail = exc.read().decode(errors='replace').replace(TOKEN, '[REDACTED]') if TOKEN else ''
    print('CURSEFORGE_HTTP_ERROR', exc.code, detail[:1200], file=sys.stderr)
    raise SystemExit(1)
except Exception as exc:
    state['status'] = 'uncertain' if state.get('status') == 'uploading' else 'failed_before_upload'
    state['error'] = str(exc).replace(TOKEN, '[REDACTED]') if TOKEN else str(exc)
    save()
    print(type(exc).__name__ + ': ' + state['error'], file=sys.stderr)
    raise SystemExit(1)
