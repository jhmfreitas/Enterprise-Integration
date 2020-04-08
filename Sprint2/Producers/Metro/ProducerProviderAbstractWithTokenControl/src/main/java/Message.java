
public class Message {

	private int ID;
	private String token;
	private String asText;
	private String timeStamp;
	private String operation;
	private String Station;
	
	public String toString()
	{
		String result = new String();
		
		result = "ID=" + getID() +
				 " token=" + getToken() +
				 " timeStamp=" + getTimeStamp() +
				 " operation=" + getOperation() +
				 " Station=" + getStation() +
				 " asText=" + getAsText();
		
		
		return result;
	}
	
	public int getID() {
		return ID;
	}
	public void setID(int iD) {
		ID = iD;
	}
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	public String getAsText() {
		return asText;
	}
	public void setAsText(String asText) {
		this.asText = asText;
	}
	public String getTimeStamp() {
		return timeStamp;
	}
	public void setTimeStamp(String timeStamp) {
		this.timeStamp = timeStamp;
	}
	public String getOperation() {
		return operation;
	}
	public void setOperation(String operation) {
		this.operation = operation;
	}
	public String getStation() {
		return Station;
	}
	public void setStation(String station) {
		Station = station;
	}
	
	public Message()
	{
		token = new String();
		asText = new String();
		timeStamp = new String();
		operation = new String();
		Station = new String();
	}
	
	
	
}
