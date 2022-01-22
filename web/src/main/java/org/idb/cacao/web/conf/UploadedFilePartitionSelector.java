package org.idb.cacao.web.conf;

import org.springframework.cloud.stream.binder.PartitionSelectorStrategy;

public class UploadedFilePartitionSelector implements PartitionSelectorStrategy {

	@Override
	public int selectPartition(Object key, int partitionCount) {
		return 0;
	}

}
