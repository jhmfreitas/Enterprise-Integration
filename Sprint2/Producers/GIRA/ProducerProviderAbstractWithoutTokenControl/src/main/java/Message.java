
public class Message {

	private int ID;
	private String token;
	private String asText;
	private String timeStamp;
	private String operation;
	private float price;
	private int time;
	
	public String toString()
	{
		String result = new String();
		
		result = "ID=" + getID() +
				 " token=" + getToken() +
				 " timeStamp=" + getTimeStamp() +
				 " operation=" + getOperation() +
				 " Price=" + getPrice() +
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
	
	public int getTime() {
		return time;
	}
	
	public void setTime(int time) {
		this.time = time;
	}
	public String getOperation() {
		return operation;
	}
	public void setOperation(String operation) {
		this.operation = operation;
	}
	
	public Message()
	{
		token = new String();
		asText = new String();
		timeStamp = new String();
		operation = new String();
	}

	public void setPrice(float price) {
		// TODO Auto-generated method stub
		this.price = price;
	}
	
	public float getPrice() {
		return price;
	}
	
	
}
