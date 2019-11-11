/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.dangdang.ddframe.job.util.env;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 获取真实本机网络的服务.
 * 
 * @author zhangliang
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class IpUtils {
    
    /**
     * IP地址的正则表达式.
     */
    public static final String IP_REGEX = "((\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3})";
    
    private static volatile String cachedIpAddress;
    
    public static void setHostIp(String ip) {
    	cachedIpAddress = ip;
    }
    
    /**
     * 获取本机IP地址.
     * 
     * <p>
     * 有限获取外网IP地址.
     * 也有可能是链接着路由器的最终IP地址.
     * </p>
     * 
     * @return 本机IP地址
     */
    public static String getIp() {
        if (null != cachedIpAddress) {
            return cachedIpAddress;
        }
        Enumeration<NetworkInterface> netInterfaces;
        try {
            netInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (final SocketException ex) {
            throw new HostException(ex);
        }
        InetAddress uncertain = null, address = null;
        while (netInterfaces.hasMoreElements()) {
            NetworkInterface netInterface = netInterfaces.nextElement();
            Enumeration<InetAddress> ipAddresses = netInterface.getInetAddresses();
            while (ipAddresses.hasMoreElements()) {
                InetAddress ipAddress = ipAddresses.nextElement();
                if (isPublicIpAddress(ipAddress)) {
                	if (isRealAddress(ipAddress)) {
                		address = ipAddress;
                		break;
                	} else {
                		uncertain = ipAddress;
                	}
                }
//                if (isLocalIpAddress(ipAddress)) {
//                    localIpAddress = ipAddress.getHostAddress();
//                }
            }
        }
        if (address == null && uncertain != null) {
        	address = uncertain;
        } 
        if (address == null) {
        	try {
				address = InetAddress.getLocalHost();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
        }
        cachedIpAddress = (address != null) ? address.getHostAddress() : "127.0.0.1";
        return cachedIpAddress;
    }
    
    private static boolean isPublicIpAddress(final InetAddress ipAddress) {
        return //!ipAddress.isSiteLocalAddress() && 
        		!ipAddress.isLoopbackAddress() && !isV6IpAddress(ipAddress);
    }
    
//    private static boolean isLocalIpAddress(final InetAddress ipAddress) {
//        return ipAddress.isSiteLocalAddress() && !ipAddress.isLoopbackAddress() && !isV6IpAddress(ipAddress);
//    }
    
    private static boolean isV6IpAddress(final InetAddress ipAddress) {
        return ipAddress.getHostAddress().contains(":");
    }
    private static boolean isRealAddress(InetAddress address) {
		String ipv4 = address.getHostAddress();   //IPV4
		String segments = ipv4.substring(0, ipv4.lastIndexOf("."));
		return Pattern.compile(IP_REGEX).matcher(ipv4).matches() && segments.length() > 5;
	}
    
    /**
     * 获取本机Host名称.
     * 
     * @return 本机Host名称
     */
    public static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (final UnknownHostException ex) {
            throw new HostException(ex);
        }
    }
}
