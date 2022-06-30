/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.conf;

import org.springframework.cloud.stream.binder.PartitionKeyExtractorStrategy;
import org.springframework.cloud.stream.binder.PartitionSelectorStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

/**
 * Configuration related to use of CloudStream
 * 
 * @author Gustavo Figueiredo
 *
 */
@Configuration
public class StreamConfiguration {
	
	/**
	 * Property name that may be set as 'header' of a 'Message' in order to determine the
	 * exact partition of KAFKA.
	 * It's also important to set the application property 'spring.cloud.stream.bindings.<binding-name>.producer.partition-count'
	 * to some value greater than this number. Otherwise this will be useless and only the partition 0 will be used.
	 */
	public static final String HEADER_PARTITION = "partitionId";
	
	/**
	 * Wrapper class for setting the exact partition number to be used in produced message
	 * @author Gustavo Figueiredo
	 */
	private static class Partition {
		private final int partition;
		Partition(int partition) {
			this.partition = partition;
		}
		public String toString() {
			return String.valueOf(partition);
		}
	}
	
	/**
	 * Bean configured as application property 'spring.cloud.stream.bindings.<binding-name>.producer.partition-key-extractor-name' to
	 * provide a key out of a produced message. This key will be used later for determining the partition.
	 */
	@Bean("UploadedFilePartitioner")
	public PartitionKeyExtractorStrategy partitionKeyExtractorStrategy() {
		return new PartitionKeyExtractorStrategy() {

			/**
			 * If the message includes the header 'partitionId', use this number for determining the exact partition. Otherwise
			 * use the payload as 'key' (if it's a String or a Number) or the hashcode (if it's not). 
			 */
			@Override
			public Object extractKey(Message<?> message) {
				Integer partition = (Integer)message.getHeaders().get(HEADER_PARTITION);
				if (partition!=null)
					return new Partition(partition);
				
				Object payload = message.getPayload();
				if ((payload instanceof String) || (payload instanceof Number)) {
					return payload;
				}
				return payload.hashCode();
				
			}
			
		};
	}
	
	/**
	 * Bean configured as application property 'spring.cloud.stream.bindings.<binding-name>.producer.partition-selector-name' to provide
	 * a partition number out of a key.
	 */
	@Bean("UploadedFileSelector")
	public PartitionSelectorStrategy partitionSelectorStrategy() {
		return new PartitionSelectorStrategy() {

			/**
			 * If the key is an instance of 'Partition', use this information as the partition to be returned (capped to the
			 * maximum configured as application property 'spring.cloud.stream.bindings.<binding-name>.producer.partition-count')
			 */
			@Override
			public int selectPartition(Object key, int partitionCount) {
				if (key instanceof Partition)
					return ((Partition)key).partition % partitionCount;

				else if (key==null)
					return 0;
				
				else
					return key.hashCode() % partitionCount;
			}
			
		};
	}
}
