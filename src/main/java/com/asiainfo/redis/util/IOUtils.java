package com.asiainfo.redis.util;

import java.io.Closeable;

/**   
 * IOUtils 流处理工具类
 * 
 * @author chenzq  
 * @date 2019年4月27日 下午5:50:12
 * @version V1.0
 * @Copyright: Copyright(c) 2019 jaesonchen.com Inc. All rights reserved. 
 */
public class IOUtils {

    public static void closeQuietly(Closeable... closeables) {
        if (null == closeables || closeables.length == 0) {
            return;
        }
        for (Closeable c : closeables) {
            try {
                c.close();
            } catch (Exception ex) {
                // ignore
            }
        }
    }
}
