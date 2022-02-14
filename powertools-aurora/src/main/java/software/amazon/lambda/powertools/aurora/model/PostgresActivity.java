/**
 * 
 */
package software.amazon.lambda.powertools.aurora.model;

import java.io.Serializable;
import java.util.Objects;

public class PostgresActivity implements Serializable {
	String type;
	String version;
	String databaseActivityEvents;
	String key;

	/**
	 * 
	 */
	public PostgresActivity() {
		super();
	}

	/**
	 * @param type
	 * @param version
	 * @param databaseActivityEvents
	 * @param key
	 */
	public PostgresActivity(String type, String version, String databaseActivityEvents, String key) {
		super();
		this.type = type;
		this.version = version;
		this.databaseActivityEvents = databaseActivityEvents;
		this.key = key;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * @param version the version to set
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * @return the databaseActivityEvents
	 */
	public String getDatabaseActivityEvents() {
		return databaseActivityEvents;
	}

	/**
	 * @param databaseActivityEvents the databaseActivityEvents to set
	 */
	public void setDatabaseActivityEvents(String databaseActivityEvents) {
		this.databaseActivityEvents = databaseActivityEvents;
	}

	/**
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * @param key the key to set
	 */
	public void setKey(String key) {
		this.key = key;
	}

	@Override
	public int hashCode() {
		return Objects.hash(databaseActivityEvents, key, type, version);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof PostgresActivity)) {
			return false;
		}
		PostgresActivity other = (PostgresActivity) obj;
		return Objects.equals(databaseActivityEvents, other.databaseActivityEvents) && Objects.equals(key, other.key)
				&& Objects.equals(type, other.type) && Objects.equals(version, other.version);
	}

	@Override
	public String toString() {
		return "PostgresActivity [type=" + type + ", version=" + version + ", databaseActivityEvents="
				+ databaseActivityEvents + ", key=" + key + "]";
	}

}
