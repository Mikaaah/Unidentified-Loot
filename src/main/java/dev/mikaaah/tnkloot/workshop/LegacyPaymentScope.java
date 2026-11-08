package dev.mikaaah.tnkloot.workshop;
/** Development regression suites explicitly exercise the retained item-currency mode. */
public final class LegacyPaymentScope implements AutoCloseable {
 private final String previous;
 public LegacyPaymentScope()throws Exception{previous=java.nio.file.Files.readString(ExperienceCosts.path());var j=com.google.gson.JsonParser.parseString(previous).getAsJsonObject();j.addProperty("identification_payment","items");j.addProperty("salvage_costs_xp",false);java.nio.file.Files.writeString(ExperienceCosts.path(),j.toString());if(!ExperienceCosts.reload())throw new IllegalStateException("Legacy payment setup failed");}
 @Override public void close()throws Exception{java.nio.file.Files.writeString(ExperienceCosts.path(),previous);if(!ExperienceCosts.reload())throw new IllegalStateException("XP settings restore failed");}
}
