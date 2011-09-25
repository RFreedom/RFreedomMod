package me.StevenLawson.TotalFreedomMod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

public class TotalFreedomMod extends JavaPlugin
{
    private final TotalFreedomModEntityListener entityListener = new TotalFreedomModEntityListener(this);
    private final TotalFreedomModBlockListener blockListener = new TotalFreedomModBlockListener(this);
    //private final TotalFreedomModPlayerListener playerListener = new TotalFreedomModPlayerListener(this);
    private static final Logger log = Logger.getLogger("Minecraft");
    protected static Configuration CONFIG;
    private List<String> superadmins = new ArrayList<String>();
    public Boolean allowExplosions = false;
    public Boolean allowLavaDamage = false;
    public Boolean allowFire = false;
    public final static String MSG_NO_PERMS = ChatColor.YELLOW + "You do not have permission to use this command.";
    public final static String YOU_ARE_OP = ChatColor.YELLOW + "You are now op!";
    public final static String YOU_ARE_NOT_OP = ChatColor.YELLOW + "You are no longer op!";

    public void onEnable()
    {
        CONFIG = getConfiguration();
        CONFIG.load();
        if (CONFIG.getString("superadmins", null) == null) //Generate config file:
        {
            log.log(Level.INFO, "[Total Freedom Mod] - Generating default config file (plugins/TotalFreedomMod/config.yml)...");
            CONFIG.setProperty("superadmins", new String[]
                    {
                        "Madgeek1450", "markbyron"
                    });
            CONFIG.setProperty("allow_explosions", false);
            CONFIG.setProperty("allow_lava_damage", false);
            CONFIG.setProperty("allow_fire", false);
            CONFIG.save();
            CONFIG.load();
        }
        superadmins = CONFIG.getStringList("superadmins", null);
        allowExplosions = CONFIG.getBoolean("allow_explosions", false);
        allowLavaDamage = CONFIG.getBoolean("allow_lava_damage", false);
        allowFire = CONFIG.getBoolean("allow_fire", false);

        PluginManager pm = this.getServer().getPluginManager();
        pm.registerEvent(Event.Type.ENTITY_EXPLODE, entityListener, Event.Priority.High, this);
        pm.registerEvent(Event.Type.ENTITY_COMBUST, entityListener, Event.Priority.High, this);
        pm.registerEvent(Event.Type.ENTITY_DAMAGE, entityListener, Event.Priority.High, this);
        pm.registerEvent(Event.Type.BLOCK_IGNITE, blockListener, Event.Priority.High, this);
        pm.registerEvent(Event.Type.BLOCK_BURN, blockListener, Event.Priority.High, this);

        log.log(Level.INFO, "[Total Freedom Mod] - Enabled! - Version: " + this.getDescription().getVersion() + " by Madgeek1450");
        log.log(Level.INFO, "[Total Freedom Mod] - Loaded superadmins: " + implodeStringList(", ", superadmins));
    }

    public void onDisable()
    {
        log.log(Level.INFO, "[Total Freedom Mod] - Disabled.");
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
    {
        Player player = null;
        if (sender instanceof Player)
        {
            player = (Player) sender;
            log.log(Level.INFO, String.format("[PLAYER_COMMAND] %s(%s): /%s %s", player.getName(), player.getDisplayName().replaceAll("\\xA7.", ""), commandLabel, implodeStringList(" ", Arrays.asList(args))));
        }
        else
        {
            log.log(Level.INFO, String.format("[CONSOLE_COMMAND] %s: /%s %s", sender.getName(), commandLabel, implodeStringList(" ", Arrays.asList(args))));
        }

        if (cmd.getName().equalsIgnoreCase("opme"))
        {
            if (player == null)
            {
                sender.sendMessage("This command only works in-game.");
            }
            else
            {
                if (isUserSuperadmin(sender.getName()))
                {
                    tfBroadcastMessage(String.format("(%s: Opping %s)", sender.getName(), sender.getName()), ChatColor.GRAY);
                    sender.setOp(true);
                    sender.sendMessage(YOU_ARE_OP);
                }
                else
                {
                    sender.sendMessage(MSG_NO_PERMS);
                }
            }

            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("listreal") || cmd.getName().equalsIgnoreCase("list"))
        {
            StringBuilder onlineStats = new StringBuilder();
            StringBuilder onlineUsers = new StringBuilder();

            if (player == null)
            {
                onlineStats.append(String.format("There are %d out of a maximum %d players online.", Bukkit.getOnlinePlayers().length, Bukkit.getMaxPlayers()));

                onlineUsers.append("Connected players: ");
                boolean first = true;
                for (Player p : Bukkit.getOnlinePlayers())
                {
                    if (first)
                    {
                        first = false;
                    }
                    else
                    {
                        onlineUsers.append(", ");
                    }

                    if (sender.getName().equalsIgnoreCase("remotebukkit"))
                    {
                        onlineUsers.append(p.getName());
                    }
                    else
                    {
                        if (p.isOp())
                        {
                            onlineUsers.append("[OP]").append(p.getName());
                        }
                        else
                        {
                            onlineUsers.append(p.getName());
                        }
                    }
                }
            }
            else
            {
                onlineStats.append(ChatColor.BLUE).append("There are ").append(ChatColor.RED).append(Bukkit.getOnlinePlayers().length);
                onlineStats.append(ChatColor.BLUE).append(" out of a maximum ").append(ChatColor.RED).append(Bukkit.getMaxPlayers());
                onlineStats.append(ChatColor.BLUE).append(" players online.");

                onlineUsers.append("Connected players: ");
                boolean first = true;
                for (Player p : Bukkit.getOnlinePlayers())
                {
                    if (first)
                    {
                        first = false;
                    }
                    else
                    {
                        onlineUsers.append(", ");
                    }

                    if (p.isOp())
                    {
                        onlineUsers.append(ChatColor.RED).append(p.getName());
                    }
                    else
                    {
                        onlineUsers.append(p.getName());
                    }

                    onlineUsers.append(ChatColor.WHITE);
                }
            }

            sender.sendMessage(onlineStats.toString());
            sender.sendMessage(onlineUsers.toString());

            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("deopall"))
        {
            if (isUserSuperadmin(sender.getName()) || player == null)
            {
                tfBroadcastMessage(String.format("(%s: De-opping everyone)", sender.getName()), ChatColor.GRAY);

                for (Player p : Bukkit.getOnlinePlayers())
                {
                    if (!isUserSuperadmin(p.getName()) && !p.getName().equals(sender.getName()))
                    {
                        p.setOp(false);
                        p.sendMessage(YOU_ARE_NOT_OP);
                    }
                }
            }
            else
            {
                sender.sendMessage(MSG_NO_PERMS);
            }

            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("opall"))
        {
            if (isUserSuperadmin(sender.getName()) || player == null)
            {
                tfBroadcastMessage(String.format("(%s: Opping everyone)", sender.getName()), ChatColor.GRAY);

                boolean doSetGamemode = false;
                GameMode targetGamemode = GameMode.CREATIVE;
                if (args.length != 0)
                {
                    if (args[0].equals("-c"))
                    {
                        doSetGamemode = true;
                        targetGamemode = GameMode.CREATIVE;
                    }
                    else if (args[0].equals("-s"))
                    {
                        doSetGamemode = true;
                        targetGamemode = GameMode.SURVIVAL;
                    }
                }

                for (Player p : Bukkit.getOnlinePlayers())
                {
                    p.setOp(true);
                    p.sendMessage(YOU_ARE_OP);

                    if (doSetGamemode)
                    {
                        p.setGameMode(targetGamemode);
                    }
                }
            }
            else
            {
                sender.sendMessage(MSG_NO_PERMS);
            }

            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("qop")) //Quick OP
        {
            if (args.length != 1)
            {
                return false;
            }

            if (sender.isOp() || player == null || isUserSuperadmin(sender.getName()))
            {
                boolean matched_player = false;
                for (Player p : Bukkit.matchPlayer(args[0]))
                {
                    matched_player = true;

                    tfBroadcastMessage(String.format("(%s: Opping %s)", sender.getName(), p.getName()), ChatColor.GRAY);
                    p.setOp(true);
                    p.sendMessage(YOU_ARE_OP);
                }
                if (!matched_player)
                {
                    sender.sendMessage("No targets matched.");
                }
            }
            else
            {
                sender.sendMessage(MSG_NO_PERMS);
            }

            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("qdeop")) //Quick De-op
        {
            if (args.length != 1)
            {
                return false;
            }

            if (sender.isOp() || player == null || isUserSuperadmin(sender.getName()))
            {
                boolean matched_player = false;
                for (Player p : Bukkit.matchPlayer(args[0]))
                {
                    matched_player = true;

                    tfBroadcastMessage(String.format("(%s: De-opping %s)", sender.getName(), p.getName()), ChatColor.GRAY);
                    p.setOp(false);
                    p.sendMessage(YOU_ARE_NOT_OP);
                }
                if (!matched_player)
                {
                    sender.sendMessage("No targets matched.");
                }
            }
            else
            {
                sender.sendMessage(MSG_NO_PERMS);
            }

            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("survival"))
        {
            if (player == null)
            {
                if (args.length == 0)
                {
                    sender.sendMessage("When used from the console, you must define a target user to change gamemode on.");
                    return true;
                }
            }
            else
            {
                if (!sender.isOp())
                {
                    sender.sendMessage(MSG_NO_PERMS);
                    return true;
                }
            }

            Player p;
            if (args.length == 0)
            {
                p = Bukkit.getPlayerExact(sender.getName());
            }
            else
            {
                List<Player> matches = Bukkit.matchPlayer(args[0]);
                if (matches.isEmpty())
                {
                    sender.sendMessage("Can't find user " + args[0]);
                    return true;
                }
                else
                {
                    p = matches.get(0);
                }
            }

            sender.sendMessage("Setting " + p.getName() + " to game mode 'Survival'.");
            p.sendMessage(sender.getName() + " set your game mode to 'Survival'.");
            p.setGameMode(GameMode.SURVIVAL);

            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("creative"))
        {
            if (player == null)
            {
                if (args.length == 0)
                {
                    sender.sendMessage("When used from the console, you must define a target user to change gamemode on.");
                    return true;
                }
            }
            else
            {
                if (!sender.isOp())
                {
                    sender.sendMessage(MSG_NO_PERMS);
                    return true;
                }
            }

            Player p;
            if (args.length == 0)
            {
                p = Bukkit.getPlayerExact(sender.getName());
            }
            else
            {
                List<Player> matches = Bukkit.matchPlayer(args[0]);
                if (matches.isEmpty())
                {
                    sender.sendMessage("Can't find user " + args[0]);
                    return true;
                }
                else
                {
                    p = matches.get(0);
                }
            }

            sender.sendMessage("Setting " + p.getName() + " to game mode 'Creative'.");
            p.sendMessage(sender.getName() + " set your game mode to 'Creative'.");
            p.setGameMode(GameMode.CREATIVE);

            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("wildcard"))
        {
            if (player == null || isUserSuperadmin(sender.getName()))
            {
                if (args[0].equals("wildcard"))
                {
                    sender.sendMessage("What the hell are you trying to do, you stupid idiot...");
                    return true;
                }

                String base_command = implodeStringList(" ", Arrays.asList(args));

                for (Player p : Bukkit.getOnlinePlayers())
                {
                    String out_command = base_command.replaceAll("\\x3f", p.getName());
                    sender.sendMessage("Running Command: " + out_command);
                    Bukkit.getServer().dispatchCommand(sender, out_command);
                }
            }
            else
            {
                sender.sendMessage(MSG_NO_PERMS);
            }

            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("say"))
        {
            if (args.length == 0)
            {
                return false;
            }

            if (player == null || sender.isOp())
            {
                String message = implodeStringList(" ", Arrays.asList(args));
                tfBroadcastMessage(String.format("[Server:%s] %s", sender.getName(), message), ChatColor.LIGHT_PURPLE);
            }
            else
            {
                sender.sendMessage(MSG_NO_PERMS);
            }

            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("gtfo"))
        {
            if (args.length != 1)
            {
                return false;
            }

            if (player == null || isUserSuperadmin(sender.getName()))
            {
                Player p;
                List<Player> matches = Bukkit.matchPlayer(args[0]);
                if (matches.isEmpty())
                {
                    sender.sendMessage("Can't find user " + args[0]);
                    return true;
                }
                else
                {
                    p = matches.get(0);
                }

                Bukkit.getServer().dispatchCommand(sender, "smite " + p.getName());

                p.setOp(false);

                String user_ip = p.getAddress().getAddress().toString().replaceAll("/", "").trim();

                tfBroadcastMessage(String.format("Banning: %s, IP: %s.", p.getName(), user_ip), ChatColor.RED);
                Bukkit.banIP(user_ip);
                Bukkit.getOfflinePlayer(p.getName()).setBanned(true);

                p.kickPlayer("GTFO");
            }
            else
            {
                sender.sendMessage(MSG_NO_PERMS);
            }

            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("stop"))
        {
            if (player == null || isUserSuperadmin(sender.getName()))
            {
                tfBroadcastMessage("Server is going offline.", ChatColor.GRAY);

                for (Player p : Bukkit.getOnlinePlayers())
                {
                    p.kickPlayer("Server is going offline, come back in a few minutes.");
                }

                Bukkit.shutdown();
            }
            else
            {
                sender.sendMessage(MSG_NO_PERMS);
            }

            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("explosives"))
        {
            if (player == null || isUserSuperadmin(sender.getName()))
            {
                if (args.length != 1)
                {
                    return false;
                }

                if (args[0].equalsIgnoreCase("on"))
                {
                    this.allowExplosions = true;
                    sender.sendMessage("Explosives are now enabled.");
                }
                else
                {
                    this.allowExplosions = false;
                    sender.sendMessage("Explosives are now disabled.");
                }
            }
            else
            {
                sender.sendMessage(MSG_NO_PERMS);
            }

            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("fire"))
        {
            if (player == null || isUserSuperadmin(sender.getName()))
            {
                if (args.length != 1)
                {
                    return false;
                }

                if (args[0].equalsIgnoreCase("on"))
                {
                    this.allowFire = true;
                    sender.sendMessage("Fire is now enabled.");
                }
                else
                {
                    this.allowFire = false;
                    sender.sendMessage("Fire is now disabled.");
                }
            }
            else
            {
                sender.sendMessage(MSG_NO_PERMS);
            }

            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("lavadmg"))
        {
            if (player == null || isUserSuperadmin(sender.getName()))
            {
                if (args.length != 1)
                {
                    return false;
                }

                if (args[0].equalsIgnoreCase("on"))
                {
                    this.allowLavaDamage = true;
                    sender.sendMessage("Lava damage is now enabled.");
                }
                else
                {
                    this.allowLavaDamage = false;
                    sender.sendMessage("Lava damage is now disabled.");
                }
            }
            else
            {
                sender.sendMessage(MSG_NO_PERMS);
            }

            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("radar"))
        {
            if (player == null)
            {
				sender.sendMessage("This command can only be used in-game.");
				return true;
            }

			Player sender_player = Bukkit.getPlayerExact(sender.getName());
			Location sender_pos = sender_player.getLocation();
			String sender_world = sender_player.getWorld().getName();

			List<RadarData> radar_data = new ArrayList<RadarData>();

			for (Player p : Bukkit.getOnlinePlayers())
			{
				if (sender_world.equals(p.getWorld().getName()))
				{
					radar_data.add(new RadarData(p, sender_pos.distance(p.getLocation())));
				}
			}

			Collections.sort(radar_data, new RadarData());

			sender.sendMessage(ChatColor.YELLOW + "People nearby in " + sender_world + ":");

			int countmax = 5;
			if (args.length == 1)
			{
				countmax = Integer.parseInt(args[0]);
			}

			int count = 0;
			for (RadarData i : radar_data)
			{
				if (count++ > countmax)
				{
					break;
				}

				sender.sendMessage(ChatColor.YELLOW + String.format("%s - %d", i.player.getName(), Math.round(i.distance)));
			}

            return true;
        }

        return false;
    }

    public static void tfBroadcastMessage(String message, ChatColor color)
    {
        log.info(message);

        for (Player p : Bukkit.getOnlinePlayers())
        {
            p.sendMessage(color + message);
        }
    }

    private static String implodeStringList(String glue, List<String> pieces)
    {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < pieces.size(); i++)
        {
            if (i != 0)
            {
                output.append(glue);
            }
            output.append(pieces.get(i));
        }
        return output.toString();
    }

    private boolean isUserSuperadmin(String userName)
    {
        return superadmins.contains(userName);
    }
}
