package br.cefetrj.sagitarii.nunki;

/**
 * Copyright 2015 Carlos Magno Abreu
 * magno.mabreu@gmail.com 
 *
 * Licensed under the Apache  License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required  by  applicable law or agreed to in  writing,  software
 * distributed   under the  License is  distributed  on  an  "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the  specific language  governing  permissions  and
 * limitations under the License.
 * 
 */

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;


public class SystemProperties  {
    private int availableProcessors;
    private String soName;
    private String localIpAddress;
    private String machineName;
    private String macAddress;
    private String javaVersion;
    private long freeMemory;
    private long totalMemory;
    private String nunkiRootFolder;
    private String nunkiJarPath;
    private String path;
	private String classPath;
	private List<Double> mediumLoad = new ArrayList<Double>();
	private final int LOADS_MEDIUM_SIZE = 100;
	private static List<Double> mediumRamLoad = new ArrayList<Double>();

	
	private double getRamLoadsMedium( double value ) {
		mediumRamLoad.add( value );
		if ( mediumRamLoad.size() > LOADS_MEDIUM_SIZE ) {
			mediumRamLoad.remove(0);
		}
		Double totalValue = 0.0;
		for ( Double val : mediumRamLoad ) {
			totalValue = totalValue + val;
		}
		return totalValue / mediumRamLoad.size();
	}	

	private double getLoadsMedium( double value ) {
		mediumLoad.add( value );
		if ( mediumLoad.size() > LOADS_MEDIUM_SIZE ) {
			mediumLoad.remove(0);
		}
		Double totalValue = 0.0;
		for ( Double val : mediumLoad ) {
			totalValue = totalValue + val;
		}
		return totalValue / mediumLoad.size();
	}	
	
	public double getMemoryPercent( ) {
		double percent = 0;
		try {
			percent = Math.round( (freeMemory * 100 ) / totalMemory );
		} catch ( Exception ignored ) {}
		return getRamLoadsMedium( percent );
	}

    private double getProcessCpuLoad() {
    	double finalValue = 0.0;
    	try {
	        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
	        ObjectName name = ObjectName.getInstance("java.lang:type=OperatingSystem");
	        AttributeList list = mbs.getAttributes(name, new String[]{ "SystemCpuLoad" });
	        if (list.isEmpty())  return 0;
	        Attribute att = (Attribute)list.get(0);
	        Double value = (Double)att.getValue();
	        if (value == -1.0) return 0; 
	        finalValue = ((int)(value * 1000) / 10.0);
    	} catch (MalformedObjectNameException | ReflectionException | InstanceNotFoundException e) {
    		//
    	}
    	return Math.ceil( getLoadsMedium( finalValue ) );
    }    

    private InetAddress getFirstNonLoopbackAddress(boolean preferIpv4, boolean preferIPv6) throws SocketException {
        Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
        while (en.hasMoreElements()) {
            NetworkInterface i = en.nextElement();
            for (Enumeration<InetAddress> en2 = i.getInetAddresses(); en2.hasMoreElements();) {
                InetAddress addr = en2.nextElement();
                if (!addr.isLoopbackAddress()) {
                    if (addr instanceof Inet4Address) {
                        if (preferIPv6) {
                            continue;
                        }
                        return addr;
                    }
                    if (addr instanceof Inet6Address) {
                        if (preferIpv4) {
                            continue;
                        }
                        return addr;
                    }
                }
            }
        }
        return null;
    }    
    
    public String getJavaVersion() {
    	return this.javaVersion;
    }

    public long getTotalMemory() {
    	totalMemory = Runtime.getRuntime().totalMemory();
		return totalMemory;
	}
    
    public long getFreeMemory() {
    	freeMemory = Runtime.getRuntime().freeMemory();
		return freeMemory;
	}
    
    public String getnunkiRootFolder() {
		return nunkiRootFolder;
	}
    
    public String getnunkiJarPath() {
		return nunkiJarPath;
	}
    
    public String getPath() {
		return path;
	}
    
    public long getFreeDiskSpace() {
    	File myself = new File( nunkiRootFolder );
    	return myself.getUsableSpace() /1024 /1024 ;
    }

    public long getTotalDiskSpace() {
    	File myself = new File( nunkiRootFolder );
    	return myself.getTotalSpace() /1024 /1024 ;
    }
   
    public SystemProperties() throws Exception {
    	
		File f = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath() );
		nunkiJarPath =  f.getAbsolutePath();
		nunkiRootFolder = nunkiJarPath.substring(0, nunkiJarPath.lastIndexOf( File.separator ) + 1).replace(File.separator, "/");
		
		path = System.getProperty( "java.library.path");

		getProcessCpuLoad();
    	this.availableProcessors = Runtime.getRuntime().availableProcessors(); 
    	getFreeMemory(); 
    	getTotalMemory();  
    	this.soName = ManagementFactory.getOperatingSystemMXBean().getName();
    	this.localIpAddress = "***";
    	this.javaVersion = System.getProperty("java.version");
    	getProcessCpuLoad();
    	InetAddress ip;
		try {
			ip = getFirstNonLoopbackAddress(true, false);
			this.localIpAddress = ip.toString().replace("/", "");
			
			this.machineName = ip.getHostName();

			NetworkInterface network = NetworkInterface.getByInetAddress(ip);
			
			byte[] mac = network.getHardwareAddress();
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < mac.length; i++) {
				sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));		
			}
			
			
			this.macAddress =  sb.toString();   
			this.macAddress = this.macAddress.substring(0, this.macAddress.length() - 2) + "NU";
			
		} catch ( SocketException e) {
		}
    	getProcessCpuLoad();
	}

    public String getMacAddress() {
    	return this.macAddress;
    }
    
    public String getMachineName() {
    	return this.machineName;
    }
    
    public Double getCpuLoad() {
    	return getProcessCpuLoad();
    }
    
    public String getLocalIpAddress() {
    	return this.localIpAddress;
    }
	
    public int getAvailableProcessors() {
    	return this.availableProcessors;
    }

    public String getSoName() {
    	return this.soName;
    }
    
    
    public String getClassPath() {
		return classPath;
	}
    
	
}
