package com.ensoftcorp.example.synchronization;
import java.util.LinkedList;
import java.util.List;

/**
 * An example producer/consumer multithreaded application. The producer and consumer
 * share a buffer, which should be accessed under synchronization.
 * 
 * To use the Demonstration Toolbox with this application,
 * 
 * 1. Import the Demonstration Toolbox project into your workspace
 * 2. Open an Atlas Interpreter view
 * 3. Index the application.
 * 4. Select the buffer
 * 5. Run 'raceCheck(selected).show'
 * 
 * See Demonstration-Toolbox/com.ensoftcorp.atlas.java.demo.synchronization.RaceCheck
 * 
 * @author Tom Deering
 *
 */
public class ProducerConsumer {
	static List<Integer> buffer = new LinkedList<Integer>();
	static Boolean productionDone = false;
	
	public static void main(String[] args){
		Thread producer = new Thread(new Producer());
		Thread consumer = new Thread(new Consumer());
		producer.start();
		consumer.start();
	}
	
	static class Producer implements Runnable{
		@Override
		public void run() {
			for(int i=0; i<10000; i++){
				synchronized(buffer){
					buffer.add(i);
				}
			}
			productionDone = true;		
		}
	}
	
	static class Consumer implements Runnable{
		@Override
		public void run() {
			while(true){
				synchronized(buffer){
					if (buffer.size() > 0){
						System.out.println(buffer.remove(0));
					}
					else if(productionDone){
						break;
					}
				}
			}
		}
	}
}
