package dev.nucleusreel;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import java.util.List;

/** Parses the actual registered tree without executing actions, opening Minecraft, or touching config. */
public final class CommandTest {
    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();Bootstrap.bootStrap();
        var dispatcher=new CommandDispatcher<FabricClientCommandSource>();
        new NucleusReelClient().registerCommands(dispatcher);
        int count=0;
        for(String root:List.of("nr","nucleusreel"))for(String suffix:List.of(""," help"," status"," on"," off"," skip"," toggle"," reload",
            " bonus on"," bonus off"," chance second 40"," chance third 50"," chance bonus-win 50"," chance bonus-empty 50",
            " sound on"," sound off"," effects on"," effects off"," coins off"," hidechat on"," duration 6"," scale 1",
            " music on"," music off"," music folder"," music list"," music volume 0.7"," music track My song.ogg",
            " secondchance 0.4"," misschance 0.5"," sound false")) {
            var result=dispatcher.parse(root+suffix,null);
            var context=result.getContext();while(context.getChild()!=null)context=context.getChild();
            if(result.getReader().canRead()||!result.getExceptions().isEmpty()||context.getCommand()==null)
                throw new AssertionError("Command failed to parse: "+root+suffix);
            count++;
        }
        for(String command:List.of("nr chance second -1","nr chance third 101","nr chance bonus-win 0.5","nr duration 0","nr scale 2")) {
            var result=dispatcher.parse(command,null);
            if(!result.getReader().canRead()&&result.getExceptions().isEmpty())throw new AssertionError("Invalid value accepted: "+command);
        }
        int blocked=0;
        for(String root:List.of("nr","nucleusreel")) {
            for(String suffix:List.of("test", "test alloy", "test claw", "test jade", "test jaderald", "test normal",
                "test fragment", "test gems", "test book", "test miss", "test miss2", "test miss3",
                "test bonus win", "test bonus empty", "test Divan's Alloy", "debug", "preview", "demo")) {
                var result=dispatcher.parse(root+" "+suffix,null);
                if(!result.getReader().canRead()&&result.getExceptions().isEmpty())
                    throw new AssertionError("Public build accepted a preview/debug command: "+root+" "+suffix);
                blocked++;
            }
            var suggestions=dispatcher.getCompletionSuggestions(dispatcher.parse(root+" ",null)).get();
            var names=suggestions.getList().stream().map(s->s.getText()).toList();
            if(!names.containsAll(List.of("help","music","sound","effects","scale","bonus")))
                throw new AssertionError("Missing normal setting suggestions: "+names);
            for(String forbidden:List.of("test","debug","preview","demo")) {
                if(names.contains(forbidden))throw new AssertionError("Public build exposes a debug suggestion: "+forbidden);
                var nested=dispatcher.getCompletionSuggestions(dispatcher.parse(root+" "+forbidden+" ",null)).get();
                if(!nested.isEmpty())throw new AssertionError("Public build exposes nested debug suggestions: "+forbidden);
            }
        }
        System.out.println(count+" normal command forms and percent bounds verified; "+blocked
            +" preview/debug forms rejected, with no debug Tab suggestions under either alias.");
    }
}
