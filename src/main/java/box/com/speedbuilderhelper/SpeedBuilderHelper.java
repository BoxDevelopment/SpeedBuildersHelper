package box.com.speedbuilderhelper;

import box.com.speedbuilderhelper.config.ConfigManager;
import box.com.speedbuilderhelper.managers.BuildTracker;
import box.com.speedbuilderhelper.managers.GameManager;
import box.com.speedbuilderhelper.managers.TimeManager;
import box.com.speedbuilderhelper.utils.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mod(modid = SpeedBuilderHelper.MODID, version = SpeedBuilderHelper.VERSION)
public class SpeedBuilderHelper {

    public static final String MODID = "speedbuilderhelper";
    public static final String VERSION = "2.0";

    public static final Logger LOG = LogManager.getLogger("SPEEDBUILDERS");
    public final static File DIRECTORY = new File(Minecraft.getMinecraft().mcDataDir + File.separator + "SpeedBuildersHelper");

    private ConfigManager configManager;
    private TimeManager timeManager;
    private GameManager gameManager;
    private BuildTracker buildTracker;

    @EventHandler
    public void init(FMLInitializationEvent event) {
        if (!DIRECTORY.exists()) {
            DIRECTORY.mkdirs();
        }

        configManager = new ConfigManager(LOG);
        timeManager = new TimeManager(LOG);
        gameManager = new GameManager();
        buildTracker = new BuildTracker(timeManager);

        ClientCommandHandler.instance.registerCommand(new CMDS(this));
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !configManager.isActivated()) return;
        gameManager.onTick();
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!configManager.isActivated()) {
            return;
        }
        String message = event.message.getUnformattedText();

        Pattern pattern = Pattern.compile("(.*) got a perfect build in (.*)s!");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find() && matcher.group(1).equals(configManager.getPlayerName())) {
            double time = Double.parseDouble(matcher.group(2));
            buildTracker.onBuildComplete(gameManager.getCurrentTheme(), gameManager.getCurrentDifficulty(), gameManager.getCurrentVariant(), time);
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public TimeManager getTimeManager() {
        return timeManager;
    }

    public BuildTracker getBuildTracker() {
        return buildTracker;
    }

    public GameManager getGameManager() {
        return gameManager;
    }
}