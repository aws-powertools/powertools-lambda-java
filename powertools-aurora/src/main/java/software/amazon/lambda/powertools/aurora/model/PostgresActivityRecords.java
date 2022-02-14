/**
 * 
 */
package software.amazon.lambda.powertools.aurora.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PostgresActivityRecords implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8723512993989716213L;
	String type;
	String clusterId;
	String instanceId;
	List<PostgresActivityEvent> databaseActivityEventList;

	/**
	 * 
	 */
	public PostgresActivityRecords() {
		super();
	}

	/**
	 * @param type
	 * @param clusterId
	 * @param instanceId
	 * @param databaseActivityEventList
	 */
	public PostgresActivityRecords(String type, String clusterId, String instanceId,
			List<PostgresActivityEvent> databaseActivityEventList) {
		super();
		this.type = type;
		this.clusterId = clusterId;
		this.instanceId = instanceId;
		this.databaseActivityEventList = new ArrayList<>(databaseActivityEventList);
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
	 * @return the clusterId
	 */
	public String getClusterId() {
		return clusterId;
	}

	/**
	 * @param clusterId the clusterId to set
	 */
	public void setClusterId(String clusterId) {
		this.clusterId = clusterId;
	}

	/**
	 * @return the instanceId
	 */
	public String getInstanceId() {
		return instanceId;
	}

	/**
	 * @param instanceId the instanceId to set
	 */
	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	/**
	 * @return the databaseActivityEventList
	 */
	public List<PostgresActivityEvent> getDatabaseActivityEventList() {
		return new ArrayList<>(databaseActivityEventList);
	}

	/**
	 * @param databaseActivityEventList the databaseActivityEventList to set
	 */
	public void setDatabaseActivityEventList(List<PostgresActivityEvent> databaseActivityEventList) {
		this.databaseActivityEventList = new ArrayList<>(databaseActivityEventList);
	}

	@Override
	public int hashCode() {
		return Objects.hash(clusterId, databaseActivityEventList, instanceId, type);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof PostgresActivityRecords)) {
			return false;
		}
		PostgresActivityRecords other = (PostgresActivityRecords) obj;
		return Objects.equals(clusterId, other.clusterId)
				&& Objects.equals(databaseActivityEventList, other.databaseActivityEventList)
				&& Objects.equals(instanceId, other.instanceId) && Objects.equals(type, other.type);
	}

	@Override
	public String toString() {
		return "PostgresActivityRecords [type=" + type + ", clusterId=" + clusterId + ", instanceId=" + instanceId
				+ ", databaseActivityEventList=" + databaseActivityEventList + "]";
	}

}
