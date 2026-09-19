package com.travelbird.ai.service;
public final class AiTextEscaper{private AiTextEscaper(){}public static String escape(String value){if(value==null)return null;return value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}}
