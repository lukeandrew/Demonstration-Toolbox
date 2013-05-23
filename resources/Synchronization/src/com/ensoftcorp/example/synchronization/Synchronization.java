package com.ensoftcorp.example.synchronization;
import java.util.LinkedList;
import java.util.List;


public class Synchronization {
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
