import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class MaaSMessageProviderWithoutTokenControl {

	static String providerName = null;

	static String brokerList = "localhost:9092";
	static String topic = null;
	static ArrayList<String> ids = new ArrayList<String>();
	static ArrayList<Boolean> freeids = new ArrayList<Boolean>();
	static int throughput = 10;
	static String typeMessage = "JSON";
	
	
	
	private static Message CreateUsageMessageTaxi(String type)
	{
		Message response = new Message();
		
		String messageMetroCheckIn;
		if (type.compareTo("XML") == 0)
			messageMetroCheckIn = 	"<" + providerName + ">" +
										"<Usage>" +									
										"<Id>@id@</Id>" +									
										"<Price>@price@</Price>"+
										"<Timestamp>@timestamp@</Timestamp>" +
										"</Usage>" +
										"</" + providerName + ">";
		else		
			messageMetroCheckIn = "{\"event\":{\"eventType\":\"t1\", \"operator\":\"Uber\", \"info\":{ " +
								"\"Id\": \"@id@\", "+
								"\"Price\": \"@price@\", "+
								"\"Timestamp\": \"@timestamp@\" "+
							"}"+
						"}"+
				"}";
						 		
		Random rand = new Random(); 
		// Incondicional
		int position = rand.nextInt(ids.size());
		String timest = new Timestamp(System.currentTimeMillis()).toString();
		float price = rand.nextFloat()* (float) 100.0;

		response.setID(position);
		response.setOperation("Usage");
		response.setPrice(price);
		response.setTimeStamp(timest);
		response.setid(ids.get(position));
		
		response.setAsText(messageMetroCheckIn.replaceAll("@id@" , ids.get(position)));
		response.setAsText(response.getAsText().replaceAll("@timestamp@" , timest ));
		response.setAsText(response.getAsText().replaceAll("@price@" , new Float(price).toString() ));
		
		return(response);
	}
	

	private static void CheckArguments()
	{
		System.out.println("--provider-name=" + providerName + "\n" +
						   "--broker-list=" + brokerList + "\n" +
						   "--topic=" + topic + "\n" +
						   "--id-list=" + ids.toString() + "\n" +
						   "--throughput=" +  throughput + "\n" +		
						   "--typeMessage=" + typeMessage);
	}
	
		
	


	
	private static boolean VerifyArgs(String[] cabecalho)
	{
		boolean result = true;
		boolean mandatorytopic = false;
		boolean mandatoryidlist = false;
		boolean mandatoryprovidername = false;
		
		for (int i=0 ; i < cabecalho.length ; i=i+2)
		{
//			System.out.println("i=" + i + " = " + cabecalho[i]);
//			System.out.println("i=" + i+1 + " = " + cabecalho[i+1]);
			if (cabecalho[i].compareTo("--broker-list") == 0) brokerList = cabecalho[i+1];
			else if (cabecalho[i].compareTo("--provider-name") == 0)
			{
				providerName = cabecalho[i+1];
				mandatoryprovidername = true;
			}
			else if (cabecalho[i].compareTo("--topic") == 0) 
			{
					topic = cabecalho[i+1];
					mandatorytopic = true;
			}
			else if (cabecalho[i].compareTo("--id-list") == 0) 
			{
				String[] list = cabecalho[i+1].split(",");
				for (String id:list) 
				{
						ids.add(id);
						freeids.add(new Boolean(true));
				}
				mandatoryidlist = true;
			}
			else if (cabecalho[i].compareTo("--throughput") == 0) throughput = Integer.valueOf(cabecalho[i+1]).intValue();
			else if (cabecalho[i].compareTo("--typeMessage") == 0) typeMessage = cabecalho[i+1];
			else 
			{
				System.out.println("Bad argument name: " + cabecalho[i]);
				return(false);
			}
		}		
		if (mandatorytopic && mandatoryidlist && mandatoryprovidername)	return(result);
		else if (mandatoryprovidername == false) System.out.println ("Provider name argument is mandatory!");
		else if (mandatorytopic == false) System.out.println ("Topic argument is mandatory!");
		else System.out.println ("id list argument is mandatory!");
			
		return (false);
	}
	
	public static void main(String[] args) {

		String usage = "The usage of Provider Producer for MaaS Simulator is the following.\n" 
				+ "MaaSMessageProviderWithoutidControl --provider-name <Name to assign to the provider> --broker-list <KafkaBrokerList with Ports> --topic <topic> --id-list <id-list> --throughput <value> --typeMessage <value>\n"
				+ "where, \n"
				+ "--provider-name: is the name of the provider and is mandatory\n"
				+ "--broker-list: is a broker list with ports (e.g.: kafka02.example.com:9092,kafka03.example.com:9092) and the default value is localhost:9092\n"
				+ "--topic: is the kafka topic to be provisioned and is mandatory\n"
				+ "--id-list: is a list of client ids (e.g.: eudij3674fgo,dhjsyuyfdhi3,djkfjd8) and is mandatory\n"
				+ "--throughput: is the approximate maximum messages to be produced by minute and the default value is 10\n"
				+ "--typeMessage: is the type of message to be produced: JSON or XML, default value is JSON.\n";
		

		
		Properties kafkaProps = new Properties();
		if (args.length == 0) System.out.println(usage);
		else 
		{
			if (VerifyArgs(args))
			{		
				System.out.println ("The following arguments are accepted:");
				CheckArguments();
				System.out.println ("------- Processing starting -------");
				
		
				
				kafkaProps.put("bootstrap.servers", brokerList); 
				kafkaProps.put("key.serializer","org.apache.kafka.common.serialization.StringSerializer"); 
				kafkaProps.put("value.serializer","org.apache.kafka.common.serialization.StringSerializer"); 
				KafkaProducer<String, String> producer = new KafkaProducer<String, String>(kafkaProps);
				
				
				
				while (true)
				{
					try {
						Message messageToSend = new Message();
						messageToSend = CreateUsageMessageTaxi(typeMessage);
							

						
						System.out.println("This is the message to send = " + messageToSend.getAsText());
							
						
						Timestamp mili = new Timestamp(System.currentTimeMillis());
						String seqkey = new Long(mili.getTime()).toString();
						
						System.out.println("Sending new message to Kafka... with key=" + seqkey);
						
						ProducerRecord<String, String> record = new ProducerRecord<>(topic, seqkey, messageToSend.getAsText());
						producer.send(record);
						
						System.out.println("Sent...");
						
						Timestamp timestamp = new Timestamp(System.currentTimeMillis());
						System.out.println("Waiting..." + timestamp );
						Thread.sleep(60000/throughput);
					}
					catch (Exception e) { e.printStackTrace();}
					
					System.out.println("Fire-and-forget stopped.");
				}
			}
			else System.out.println("Application Arguments bad usage.\n\nPlease check syntax.\n\n" + usage);
		}
		
		
	}

}
