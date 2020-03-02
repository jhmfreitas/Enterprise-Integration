package services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import javax.xml.parsers.*;
import java.io.*;
import java.math.BigDecimal;

import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

public class CustomerManagementService {

	
	static String event = "";
	static String tariff = "";
	static int userId = 0;
	static String timestamp = "";
	static double studentDiscount = 0.0;
	static double operatorPrice = 0.0;
	static double fixedPrice = 0.0;
	
	public static void validateEventStructure(Element root, String topic, Connection conn) {
		
		System.out.println(" Start Structure validation for " + tariff + " in " + topic + ". \n");

		String auxTopic = topic.replace("Monitor", ""); 

		try {
			if(event.equals("checkOut")) {
				PreparedStatement s = null; //so uma inicializacao
				s = conn.prepareStatement("SELECT substr(events_structure, instr(events_structure,'|')+1, length(events_structure)) events_structure FROM operator_services WHERE operator =\"" + auxTopic + "\" AND tariff =\"" + tariff + "\"");
				ResultSet rsetEventStructure = s.executeQuery();
				if(rsetEventStructure.next()) {			
					
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					DocumentBuilder builder = factory.newDocumentBuilder();
					ByteArrayInputStream input = new ByteArrayInputStream(rsetEventStructure.getString("events_structure").getBytes("UTF-8"));
					
					Document doc = builder.parse(input);
					Element auxRoot = doc.getDocumentElement();
											
					NodeList rootElements = root.getElementsByTagName("*");
					NodeList auxRootElements = auxRoot.getElementsByTagName("*");
					
					if(auxRootElements.getLength() == rootElements.getLength())
					{	
						for ( int i = 0; i < auxRootElements.getLength(); i++)
						{
							if(!(auxRootElements.item(i).getNodeName().equals(rootElements.item(i).getNodeName()))) {
								System.out.println(" Invalid Structure for " + tariff + " in " + topic + ". (name) \n");
								throw new RuntimeException(" Invalid Structure for " + tariff + " in " + topic + ". (name) ");
							}
						}
					}
					else
					{
						System.out.println(" Invalid Structure for " + tariff + " in " + topic + ". (size) \n");
						throw new RuntimeException(" Invalid Structure for " + tariff + " in " + topic + ". (size) ");
					}
	
					System.out.println(" Structure Validated for " + tariff + " in " + topic + ". \n");
					
					
				}
				else
				{
					System.out.println(" tariff " + tariff + " does not exist for " + topic + ". \n");
					throw new RuntimeException(" tariff " + tariff + " does not exist for " + topic + ".");

				}
			}
			else if(event.equals("checkIn"))
			{ 	//checkIn
				PreparedStatement s = null; //so uma inicializacao
				s = conn.prepareStatement("SELECT substr(events_structure, 1, instr(events_structure,'|')-1) events_structure FROM operator_services WHERE operator =\"" + auxTopic + "\" AND tariff =\"" + tariff + "\"");
				ResultSet rsetEventStructure = s.executeQuery();
				if(rsetEventStructure.next()) {			
					
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					DocumentBuilder builder = factory.newDocumentBuilder();
					ByteArrayInputStream input = new ByteArrayInputStream(rsetEventStructure.getString("events_structure").getBytes("UTF-8"));
					
					Document doc = builder.parse(input);
					Element auxRoot = doc.getDocumentElement();
					NodeList rootElements = root.getElementsByTagName("*");
					NodeList auxRootElements = auxRoot.getElementsByTagName("*");
					
					if(auxRootElements.getLength() == rootElements.getLength())
					{	
						for ( int i = 0; i < auxRootElements.getLength(); i++)
						{
							if(!(auxRootElements.item(i).getNodeName().equals(rootElements.item(i).getNodeName()))) {
								System.out.println(" Invalid Structure for " + tariff + " in " + topic + ". (name) \n");
								throw new RuntimeException(" Invalid Structure for " + tariff + " in " + topic + ". (name) ");
							}
						}
					}
					else
					{
						System.out.println(" Invalid Structure for " + tariff + " in " + topic + ". (size) \n");
						throw new RuntimeException(" Invalid Structure for " + tariff + " in " + topic + ". (size) ");
					}
					System.out.println(" Structure Validated for " + tariff + " in " + topic + ". \n");
				}
				else
				{
					System.out.println(" tariff " + tariff + " does not exist for " + topic + ". \n");
					throw new RuntimeException(" tariff " + tariff + " does not exist for " + topic + ".");
				}
			}
			else
			{
				System.out.println(" Event Type does not Exist - " + event + " \n");
				throw new RuntimeException(" Event Type does not Exist -" + event + " \n");
			}
		} catch (SQLException | ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void parseMessage(String message, String topic, Connection conn) {
		
	
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			ByteArrayInputStream input = new ByteArrayInputStream(message.getBytes("UTF-8"));
			Document doc = builder.parse(input);
			
			Element root = doc.getDocumentElement();
			
			event = root.getNodeName();	// checkIn || checkOut
			tariff = root.getElementsByTagName("tariff").item(0).getTextContent(); // operatorPrice || fixedPrice || variablePrice

			validateEventStructure(root, topic, conn);
			
			if (root.getElementsByTagName("userId") != null && root.getElementsByTagName("userId").item(0) != null)
				userId =  Integer.parseInt(root.getElementsByTagName("userId").item(0).getTextContent()); 	// Id do user
			if (root.getElementsByTagName("timestamp") != null && root.getElementsByTagName("timestamp").item(0) != null)
				timestamp = root.getElementsByTagName("timestamp").item(0).getTextContent();	// timestamp
			if (root.getElementsByTagName("hasStudentDiscount") != null && root.getElementsByTagName("hasStudentDiscount").item(0) != null)
				studentDiscount = Double.parseDouble(root.getElementsByTagName("hasStudentDiscount").item(0).getTextContent()); 
			if (root.getElementsByTagName("operatorPrice") != null && root.getElementsByTagName("operatorPrice").item(0) != null)
				operatorPrice = Double.parseDouble(root.getElementsByTagName("operatorPrice").item(0).getTextContent()); 
			if (root.getElementsByTagName("fixedPrice") != null && root.getElementsByTagName("fixedPrice").item(0) != null)
				fixedPrice = Double.parseDouble(root.getElementsByTagName("fixedPrice").item(0).getTextContent()); 
			
			
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}

	}
	
	public static double calculatePriceOfTrip (Timestamp checkIn, Timestamp checkOut) {
		
		long in = checkIn.getTime();
		long out = checkOut.getTime();
		double price = (double)(out - in) / 1000000;
		
		return price;
	}
	
	public static double myRound (double dDouble) { // Arredonda o valor para 2 casas decimais
		
		BigDecimal bd = new BigDecimal(dDouble);
		bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
		
		return bd.doubleValue();
	}
	
	public static String timestampToDate (Timestamp timestamp) {
		
		Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp.getTime());
        Date date = calendar.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		
        return sdf.format(date);
	}
	
	public static String getRevenueTopic (String topic) {
		String transportType = topic.substring(7);	//Sacar o tipo de transporte
		
		return "Revenue" + transportType;
	}
	
	private static class DemoProducerCallback implements Callback {
		@Override
		public void onCompletion(RecordMetadata recordMetadata, Exception e) {
			if (e != null) { 
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		
		System.out.println(" Starting Customer Management Service \n");
		
		Properties props = new Properties();
		props.put("bootstrap.servers", "18.234.252.129:9092"); // IP da instancia AWS
		props.put("group.id", "MaaS");
		props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("key.serializer","org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer","org.apache.kafka.common.serialization.StringSerializer");
		props.put("auto.commit.offset", "false"); // to commit manually
		KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(props);
		KafkaProducer<String, String> producer = new KafkaProducer<String, String>(props);
		

		
		Connection conn = null;
		boolean bd_ok = false;
		
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			conn = DriverManager.getConnection(
					"jdbc:mysql://mytestdb.crjjgaudsykb.us-east-1.rds.amazonaws.com:3306/CustomerManagementService",
					"storemessages", "pedro1234"); // ("jdbc:mysql://yourAWSDBIP:3306/YOURDATABASENAME","YOURMasterUSERNAME","YOURPASSWORD")												
			bd_ok = true;
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
			
		try {
			

			// Meter o subscriber a subscrever os topicos que queremos
			System.out.println(" GETTING OPERATORS \n");
			ArrayList<String> listOfTopics = new ArrayList<String> ();
			String monitorName = "";
			
			PreparedStatement s = null; //so uma inicializacao
			s = conn.prepareStatement("SELECT name FROM operator");
			ResultSet rsetMonitor = s.executeQuery();
			while(rsetMonitor.next()) {			

				
				monitorName = rsetMonitor.getString("name");
				monitorName = "Monitor" + monitorName;
				listOfTopics.add(monitorName);
				
				System.out.println(" ADDED " + monitorName + ". \n");
				
			}
				
			consumer.subscribe(listOfTopics);
			
			System.out.println(" Starting while (true) Cycle \n");
			
			while (true) {
				ConsumerRecords<String, String> records = consumer.poll(100);
				
				for (ConsumerRecord<String, String> record : records) {			
					
					String message = record.value();	//mensagem em formato XML
					String topic = record.topic();	//topico onde a mensagem foi publicada
					long offset = record.offset();
					
					parseMessage(message, topic, conn);	//Partir a mensagem XML em strings
	
					
					if (bd_ok) {

						
 						
						switch(event) {
							case "checkIn":
								System.out.println(" ******* Start Consuming checkIn ******* \n");
								
								System.out.println(" INSERT INTO AccountManager "
										+ "VALUES(" + topic + ", " + offset + ", " + userId + ", " + timestamp + ", null, null, null) \n");
								
								s = conn.prepareStatement("INSERT INTO AccountManager VALUES(?,?,?,?,?,?,?)");
								
								s.setString(1, topic);	// topic
								s.setLong(2, offset);	// offset
								s.setInt(3, userId);	// user_id
								s.setTimestamp(4, java.sql.Timestamp.valueOf(timestamp));	// checkin_ts
								s.setTimestamp(5, null);	// checkout_ts
								s.setString(6, null);	// price
								s.setString(7, null);	// discount
								s.executeUpdate();
								
								System.out.println(" +++++ Finished Consuming checkIn +++++ \n ");
								break;
								
							case "checkOut":
								
								System.out.println(" ******* Start Consuming checkOut ******* \n");
								// checkOut --> update na entrada do ultimo checkIn (completar a informacao que falta)
								
								//Comecamos por ir buscar o offset do ultimo checkIn do user no transporte
								long maxOffset = -1;	// -1 nao quer dizer nada; so para inicializar a variavel
								
								s = conn.prepareStatement("SELECT MAX(offset) " + 
														  "FROM AccountManager " + 
														  "WHERE topic=\"" + topic + "\" AND user_id=" + userId
														  );
								ResultSet maxOffsetFromQuery = s.executeQuery();
	
								while (maxOffsetFromQuery.next()) 
									maxOffset = maxOffsetFromQuery.getLong("MAX(offset)");	//Offset maximo de um dado user num dado topico. 
																							//Equivale ao offset do seu ultimo checkIn
								System.out.println("maxOffset: " + maxOffset);
								//Ir buscar o timestamp do checkIn para poder calcular o preco a pagar
								Timestamp checkInTimestamp = null;
								
								s = conn.prepareStatement("SELECT checkin_ts " + 
										  				  "FROM AccountManager " + 
										  				  "WHERE topic=\"" + topic + "\" AND user_id=" + userId + " AND offset=" + maxOffset
										  				 );
								ResultSet checkInTimestampFromQuery = s.executeQuery();
								
								while (checkInTimestampFromQuery.next()) 
									checkInTimestamp = checkInTimestampFromQuery.getTimestamp("checkin_ts");
								
								Timestamp checkOutTimestamp = java.sql.Timestamp.valueOf(timestamp);	// O timestamp do checkout vem no evento
								
								System.out.println("checkInTimestamp: " + checkInTimestamp + " | checkOutTimestamp: " + checkOutTimestamp);
								
								double price = 0.0;
								double discount = studentDiscount;
								
								if(tariff.equals("operatorPrice"))
									price = operatorPrice;
								if(tariff.equals("variablePrice"))
									price = calculatePriceOfTrip(checkInTimestamp, checkOutTimestamp);
								if(tariff.equals("fixedPrice"))
									price = fixedPrice;
								
								price = myRound(price);
								discount = price * discount;
								discount = myRound(discount);	

								System.out.println("UPDATE AccountManager " + 
										  "SET offset= " + offset + ", checkout_ts= " + checkOutTimestamp + ", price= " + price + ", discount= " + discount + 
										  " WHERE topic= " + topic + " AND user_id= " + userId + " AND offset= " + maxOffset + "\n");
								
								s = conn.prepareStatement("UPDATE AccountManager " + 
														  "SET offset= ?, checkout_ts=?, price=?, discount=? " + 
														  "WHERE topic=? AND user_id=? AND offset=?"
														 );
								s.setLong(1, offset);
								s.setTimestamp(2, checkOutTimestamp);
								s.setDouble(3, price);
								s.setDouble(4, discount);
								s.setString(5, topic);
								s.setInt(6, userId);
								s.setLong(7, maxOffset);
								s.executeUpdate();
								
								System.out.println(" +++++ Finished Consuming checkOut +++++ \n");
								
								System.out.println(" ----- Producing paymentInfo ----- \n");

						        String checkOutDate = timestampToDate(checkOutTimestamp);	//Devolve uma data no formato "2017-06-15"
						        
								double revenue = price - discount;
								revenue = myRound(revenue);
								
								String revenueOperatorTopic = getRevenueTopic(topic);
								
								System.out.println("Sending message: "
										+ "<paymentInfo><price>" + price + "</price><discount>" + discount + "</discount><revenue>" + revenue + "</revenue><date>" + checkOutDate + "</date></paymentInfo>\n"
										+ "To topic: " + revenueOperatorTopic);

								ProducerRecord<String, String> revenueMessage = new ProducerRecord<>(revenueOperatorTopic, "MaaS", "<paymentInfo><price>" + price + "</price><discount>" + discount + "</discount><revenue>" + revenue + "</revenue><date>" + checkOutDate + "</date></paymentInfo>");
																
								try{ 
									producer.send(revenueMessage, new DemoProducerCallback());	//Asynchronous send
								} 
								catch (Exception e){ 
									e.printStackTrace();
								} 
								break;
								
								default:
									System.out.println("The event received is neither a checkIn nor a checkOut!");
						}
						s.close();
					}	
				}
				
				try {	//Commit Current Offset
					consumer.commitSync();
				} catch (CommitFailedException e) {
					System.out.printf("commit failed", e);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			consumer.close();
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}