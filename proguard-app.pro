-dontshrink
-dontoptimize
-ignorewarnings
-useuniqueclassmembernames
-overloadaggressively
-repackageclasses com.sync.sc.o
-allowaccessmodification
-keepdirectories

# Java 21 needs valid StackMapTable. Do NOT use -dontpreverify.
# Keep only runtime metadata required by Spring/Jackson binding; remove source file, line and local variable tables.
-keepattributes *Annotation*,RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations,AnnotationDefault,Signature,InnerClasses,EnclosingMethod,Record,Exceptions,MethodParameters,StackMapTable

# jpackage/main-class requires this exact entry class name.
-keep class com.sync.sc.ScFXApp { *; }
-keep class com.sync.sc.ScFXApp$* { *; }

# Configuration classes keep names and @Bean method names; Spring Boot rejects overloaded obfuscated @Bean methods.
-keep class com.sync.sc.config.** { *; }
-keep,allowobfuscation class com.sync.sc.entity.** { *; }
-keep,allowobfuscation class com.sync.sc.util.SpringContextUtil { *; }

# Controllers: allow class/package/member name obfuscation, but keep annotations and signatures.
# Endpoint URLs come from @RequestMapping/@GetMapping annotations, not Java class names.
-keep,allowobfuscation class com.sync.sc.controller.** { *; }

# Spring-managed Service/Component classes may be obfuscated. Keep members present, but allow their names to change.
-keep,allowobfuscation @org.springframework.stereotype.Service class * { *; }
-keep,allowobfuscation @org.springframework.stereotype.Component class * { *; }
-keep,allowobfuscation @org.springframework.context.annotation.Configuration class * { *; }

# Keep annotated members required by Spring MVC, DI and lifecycle. Names may still be obfuscated where safe.
-keepclassmembers,allowobfuscation class * {
    @org.springframework.beans.factory.annotation.Autowired *;
    @jakarta.annotation.Resource *;
    @org.springframework.beans.factory.annotation.Value *;
    @org.springframework.web.bind.annotation.* *;
    @org.springframework.context.annotation.Bean *;
    @jakarta.annotation.PostConstruct *;
    public <init>(...);
}

# Keep JavaBean accessors for DTO/entity binding; class names may still be obfuscated.
-keepclassmembers class com.sync.sc.entity.** {
    public void set*(...);
    public *** get*();
    public boolean is*();
}

# Keep enum helpers.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
