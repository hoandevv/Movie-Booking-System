package com.trainning.movie_booking_system.utils.constraint;

public class MailConstants {
    public static final int CORE_POOL_SIZE = 2;
    public static final int MAX_POOL_SIZE = 5;
    public static final int QUEUE_CAPACITY = 100;
    public static final String THREAD_NAME_PREFIX = "Email-";
    public static final String PREFIX = "templates/";
    public static final String SUFFIX = ".html";
    public static final String ENCODING = "UTF-8";
    public static final boolean CACHEABLE = true;
    public static final long CACHE_TTL_MS = 3_600_000L;
}


