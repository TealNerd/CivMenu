package vg.civcraft.mc.civmenu.datamanager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import vg.civcraft.mc.civmenu.CivMenu;
import vg.civcraft.mc.civmenu.TermObject;
import vg.civcraft.mc.civmenu.database.Database;
import vg.civcraft.mc.civmodcore.Config;
import vg.civcraft.mc.civmodcore.annotations.CivConfig;
import vg.civcraft.mc.civmodcore.annotations.CivConfigType;
import vg.civcraft.mc.civmodcore.annotations.CivConfigs;

public class MysqlManager implements ISaveLoad{

	private CivMenu plugin;
	private Config config;
	private Database db;
	
	private String insertData, getAllData;
	
	private Map<UUID, TermObject> registeredPlayers = new HashMap<UUID, TermObject>();
	
	public MysqlManager(CivMenu plugin) {
		this.plugin = plugin;
		config = plugin.GetConfig();
		initializeStrings();
	}
	
	private void initializeStrings() {
		insertData = "insert into civ_menu_data(uuid, term) values (?,?);";
		getAllData = "select * from civ_menu_data;";
	}
	
	@CivConfigs({
		@CivConfig(name = "mysql.username", def = "bukkit", type = CivConfigType.String),
		@CivConfig(name = "mysql.password", def = "", type = CivConfigType.String),
		@CivConfig(name = "mysql.host", def = "localhost", type = CivConfigType.String),
		@CivConfig(name = "mysql.dbname", def = "bukkit", type = CivConfigType.String),
		@CivConfig(name = "mysql.port", def = "3306", type = CivConfigType.Int)
	})
	private void loadDB() {
		String username = config.get("mysql.username").getString();
		String password = config.get("mysql.password").getString();
		String host = config.get("mysql.host").getString();
		String dbname = config.get("mysql.dbname").getString();
		int port = config.get("mysql.port").getInt();
		db = new Database(host, port, dbname, username, password, plugin.getLogger());
		if (!db.connect()) {
			plugin.getLogger().log(Level.INFO, "Mysql could not connect, shutting down.");
			Bukkit.getPluginManager().disablePlugin(plugin);
		}
		createTables();
	}
	
	private void createTables() {
		db.execute("create table if not exists civ_menu_data("
				+ "uuid varchar(36) not null,"
				+ "term varchar(255) not null,"
				+ "primary key uuid_info(uuid, term));");
	}

	@Override
	public void load() {
		loadDB();
		PreparedStatement playerData = db.prepareStatement(getAllData);
		ResultSet set;
		try {
			set = playerData.executeQuery();
			while (set.next()) {
				UUID uuid = UUID.fromString(set.getString("uuid"));
				if (!registeredPlayers.containsKey(uuid)) 
					registeredPlayers.put(uuid, new TermObject(uuid));
				registeredPlayers.get(uuid).addTerm(set.getString("term"));
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void save() {
		// No need to save, all done on playeradd.
	}

	@Override
	public void addPlayer(Player p, String term) {
		if (!registeredPlayers.containsKey(p.getUniqueId()))
			registeredPlayers.put(p.getUniqueId(), new TermObject(p.getUniqueId()));
		registeredPlayers.get(p.getUniqueId()).addTerm(term);
		PreparedStatement addPlayer = db.prepareStatement(insertData);
		try {
			addPlayer.setString(1, p.getUniqueId().toString());
			addPlayer.setString(2, term);
			addPlayer.execute();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void setUUID(UUID uuid, String term){
		if (!registeredPlayers.containsKey(uuid))
			registeredPlayers.put(uuid, new TermObject(uuid));
		registeredPlayers.get(uuid).addTerm(term);
	}

	@Override
	public boolean isAddedPlayer(Player p, String term) {
		return registeredPlayers.get(p.getUniqueId()) != null && registeredPlayers.get(p.getUniqueId()).hasTerm(term);
	}
}
