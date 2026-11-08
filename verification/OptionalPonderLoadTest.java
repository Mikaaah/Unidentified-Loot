/** Class verification must not resolve the optional Ponder classes on an ordinary client. */
public class OptionalPonderLoadTest {
 public static void main(String[]args)throws Exception {
  try {Class.forName("net.createmod.ponder.foundation.PonderIndex");throw new AssertionError("Ponder must be absent from this test classpath");}
  catch(ClassNotFoundException expected) {}
  var entry=Class.forName("dev.mikaaah.tnkloot.client.ponder.OptionalPonderClient");
  for(var method:entry.getDeclaredMethods())method.getParameterTypes();
  System.out.println("OPTIONAL_PONDER_LOAD_OK: guarded client entry verifies without Create or Ponder");
 }
}
