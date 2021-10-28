/*******************************************************************************
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
 * and associated documentation files (the "Software"), to deal in the Software without 
 * restriction, including without limitation the rights to use, copy, modify, merge, publish, 
 * distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or 
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS 
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR 
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN 
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers.services;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.idb.cacao.api.ComponentSystemInformation;
import org.idb.cacao.web.entities.SystemMetrics;
import org.idb.cacao.web.repositories.SystemMetricsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 * Service for collecting system resources metrics and reporting externally. Useful for diagnostics about system health status.
 * 
 * @author Gustavo Figueiredo
 *
 */
@Service
public class ResourceMonitorService implements Runnable {
	
	private static final Logger log = Logger.getLogger(ResourceMonitorService.class.getName());

	public static final int DEFAULT_DELAY_MINUTES = 20;

	@Autowired
	private SystemMetricsRepository systemMetricsRepository;

	@Autowired
	private ApplicationContext app;

	private final ScheduledExecutorService service;
	
	private static final AtomicBoolean firstReport = new AtomicBoolean(true);
	
	private int delayMinutes = DEFAULT_DELAY_MINUTES;
	
	private volatile ScheduledFuture<?> scheduled;
	
	public ResourceMonitorService() {
		service = new ScheduledThreadPoolExecutor(1,new ThreadFactory(){
			
			private final AtomicLong threadCounter = new AtomicLong();

			@Override
			public Thread newThread(Runnable r) {
		        Thread thread = new Thread(r,"ResourceMonitorThread#"+threadCounter.incrementAndGet());
		        thread.setDaemon(true);
		        return thread;
		    }

		});
	}

	public int getDelayMinutes() {
		return delayMinutes;
	}

	public void setDelayMinutes(int delayMinutes) {
		this.delayMinutes = delayMinutes;
	}

	public void start() {
		try {
			scheduled = service.scheduleWithFixedDelay(this, 0, delayMinutes, TimeUnit.MINUTES);
		}
		catch (Throwable ex) {
			log.log(Level.WARNING, "Error while starting ResourceMonitorService", ex);
		}
	}
	
	public void stop() {
		if (scheduled!=null) {
			try {
				scheduled.cancel(/*mayInterruptIfRunning*/false);
			}
			catch (Throwable ex) {
				log.log(Level.WARNING, "Error while stopping ResourceMonitorService", ex);
			}
		}
		scheduled = null;
	}

	@Override
	public void run() {
		try {
			SystemMetrics metrics = collectMetrics(app);
			
			systemMetricsRepository.save(metrics);
		}
		catch (Throwable ex) {
			log.log(Level.WARNING, "Error while performing resource monitor task", ex);
		}
	}
	
	public static SystemMetrics collectMetrics(ApplicationContext app) {
		SystemMetrics metrics = new SystemMetrics();
		metrics.setTimestamp(LocalDateTime.now().atOffset(ZoneOffset.UTC));
		try {
			metrics.setHost(InetAddress.getLocalHost().getHostName());
		} catch (UnknownHostException e) {	}
		Runtime r = Runtime.getRuntime();
		metrics.setHeapFreeBytes(r.maxMemory()-r.totalMemory()+r.freeMemory());
		metrics.setHeapUsedBytes(r.totalMemory()-r.freeMemory());
		collectPhysicalMemory(metrics);
		try {
			File temp = new File(System.getProperty("java.io.tmpdir"));
			long free_space = temp.getFreeSpace();
			long total_space = temp.getTotalSpace();
			metrics.setDiskTemporaryFilesFreeBytes(free_space);
			metrics.setDiskTemporaryFilesUsedBytes(total_space - free_space);
			metrics.setRestarted(firstReport.getAndSet(false));
		} catch (Throwable ex) { }
		return metrics;
	}
	
	public static void collectPhysicalMemory(SystemMetrics metrics) {
		try {
			MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
			Number totalPhysicalMemorySize = (Number)mBeanServer.getAttribute(new ObjectName("java.lang","type","OperatingSystem"), "TotalPhysicalMemorySize");
			Number freePhysicalMemorySize = (Number)mBeanServer.getAttribute(new ObjectName("java.lang","type","OperatingSystem"), "FreePhysicalMemorySize");
			metrics.setMemoryFreeBytes(freePhysicalMemorySize.longValue());
			metrics.setMemoryUsedBytes(totalPhysicalMemorySize.longValue()-freePhysicalMemorySize.longValue());
		}
		catch (Throwable ex) {			
		}
	}
	
	/**
	 * Collects information about the this component
	 */
	public ComponentSystemInformation collectInfoForComponent() {
		
		ComponentSystemInformation info = new ComponentSystemInformation();
		
		info.setJavaVersion(System.getProperty("java.version"));
		info.setJavaHome(System.getProperty("java.home"));
		info.setOsVersion(String.join(" ",System.getProperty("os.name"),System.getProperty("os.version")));
		
		info.setProcessorsCount(Runtime.getRuntime().availableProcessors());
		info.setProcessorsArch(System.getProperty("sun.arch.data.model") );

		SystemMetrics metrics = ResourceMonitorService.collectMetrics(app);
		if (metrics!=null) {
			info.setHeapUsed(metrics.getHeapUsedBytes());
			info.setHeapFree(metrics.getHeapFreeBytes());
			info.setMemUsed(metrics.getMemoryUsedBytes());
			info.setMemFree(metrics.getMemoryFreeBytes());
		}
		
		return info;
	}

}
