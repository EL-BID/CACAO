/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.api.utils;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.idb.cacao.api.ComponentSystemInformation;
import org.idb.cacao.api.SystemMetrics;
import org.idb.cacao.api.templates.TemplateArchetypes;
import org.springframework.data.repository.CrudRepository;

/**
 * Provides a continuous monitor of system resources.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class ResourceMonitor<T extends SystemMetrics> implements Runnable {

	private static final Logger log = Logger.getLogger(ResourceMonitor.class.getName());

	public static final int DEFAULT_DELAY_MINUTES = 20;
	
	private final Supplier<T> systemMetricsSupplier;

	private final CrudRepository<T, String> systemMetricsRepository;

	private final ScheduledExecutorService service;
	
	private static final AtomicBoolean firstReport = new AtomicBoolean(true);
	
	private int delayMinutes = DEFAULT_DELAY_MINUTES;
	
	private ScheduledFuture<?> scheduled;
	
	public ResourceMonitor(CrudRepository<T, String> systemMetricsRepository, Supplier<T> systemMetricsSupplier) {
		this.systemMetricsSupplier = systemMetricsSupplier;
		this.systemMetricsRepository = systemMetricsRepository;
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
		catch (Exception ex) {
			log.log(Level.WARNING, "Error while starting ResourceMonitorService", ex);
		}
	}
	
	public void stop() {
		if (scheduled!=null) {
			try {
				scheduled.cancel(/*mayInterruptIfRunning*/false);
			}
			catch (Exception ex) {
				log.log(Level.WARNING, "Error while stopping ResourceMonitorService", ex);
			}
		}
		scheduled = null;
	}

	@Override
	public void run() {
		try {
			T metrics = systemMetricsSupplier.get();
			collectMetrics(metrics);
			
			systemMetricsRepository.save(metrics);
		}
		catch (Exception ex) {
			log.log(Level.WARNING, "Error while performing resource monitor task", ex);
		}
	}
	
	public static void collectMetrics(SystemMetrics metrics) {
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
			long freeSpace = temp.getFreeSpace();
			long totalSpace = temp.getTotalSpace();
			metrics.setDiskTemporaryFilesFreeBytes(freeSpace);
			metrics.setDiskTemporaryFilesUsedBytes(totalSpace - freeSpace);
			metrics.setRestarted(firstReport.getAndSet(false));
		} catch (Exception ex) { }
	}
	
	public static void collectPhysicalMemory(SystemMetrics metrics) {
		try {
			MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
			Number totalPhysicalMemorySize = (Number)mBeanServer.getAttribute(new ObjectName("java.lang","type","OperatingSystem"), "TotalPhysicalMemorySize");
			Number freePhysicalMemorySize = (Number)mBeanServer.getAttribute(new ObjectName("java.lang","type","OperatingSystem"), "FreePhysicalMemorySize");
			metrics.setMemoryFreeBytes(freePhysicalMemorySize.longValue());
			metrics.setMemoryUsedBytes(totalPhysicalMemorySize.longValue()-freePhysicalMemorySize.longValue());
		}
		catch (Exception ex) {			
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

		T metrics = systemMetricsSupplier.get();
		collectMetrics(metrics);
		if (metrics!=null) {
			info.setHeapUsed(metrics.getHeapUsedBytes());
			info.setHeapFree(metrics.getHeapFreeBytes());
			info.setMemUsed(metrics.getMemoryUsedBytes());
			info.setMemFree(metrics.getMemoryFreeBytes());
		}
		
		info.setInstalledPlugins(new ArrayList<>(TemplateArchetypes.getInstalledPackages()));
		
		return info;
	}

}
