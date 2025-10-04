
# 📊 Spring Boot - Native Images 

### Gradle Task bootBuildImage for Native Images
It is easy to configure native image creations in Gradle:

#### bootBuildImage - simple way

    bootBuildImage {  
        environment = [
            "BP_NATIVE_IMAGE": "true",
        ]  
        builder = "paketobuildpacks/builder-jammy-buildpackless-tiny"
        buildpacks = [
            "paketobuildpacks/java-native-image"  
        ]
    }


#### bootBuildImage - including optimizations
But with explicit settings of environment build arguments, upx compression, another run image it is possible to get significant better results regarding build time, image size, startup time and memory consumptions during runtime.

    bootBuildImage {  
        environment = [  
            "BP_JVM_VERSION": "25",
            "BP_NATIVE_IMAGE": "true",
            "BP_NATIVE_IMAGE_BUILD_ARGUMENTS": [
                // (default) mostly-static linked executable  
                "-H:+UnlockExperimentalVMOptions -H:+StaticExecutableWithDynamicLibC",
                // no fallback if native-image generation fails
                "--no-fallback",
                // optimize for host machine 
                "-march=native",
                // Parallel GC: Garbage-First (G1) GC based on Java HotSpot VM  
                "--gc=parallel",
                // min/max heap size for binary, tested by locally with VM options -Xms -Xmx
                "-R:MinHeapSize=32m -R:MaxHeapSize=48m",  
                // -Os: optimizations except those that can increase code or image size significantly  
                "-Os"  
            ].join(" "),
            "BP_BINARY_COMPRESSION_METHOD": "upx",  
            "BP_RUNTIME_CERT_BINDING_DISABLED": "true"
        ]  
        builder = "paketobuildpacks/builder-jammy-buildpackless-tiny"  
        buildpacks = [
            "paketobuildpacks/java-native-image"  
        ]  
        runImage = "busybox:1.36.1-glibc"  
        buildCache {  
            bind {
                source.set("/tmp/paketobuildpacks/cache-${rootProject.name}.build")  
            }  
        }  
        launchCache {  
            bind {  
                source.set("/tmp/paketobuildpacks/cache-${rootProject.name}.launch")  
            }
        }
    }


## Test Results GraalVM 21
| bootBuildImage native-image                                 | + upx compression<br> + busybox runtime<br> + defaults                                                          | + upx compression<br> + busybox runtime<br> + mem optimizations<br> + default optimizations<br> + parallel gc                                          |
|-------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| Build Environment                                           | BP_NATIVE_IMAGE<br>BP_BINARY_COMPRESSION_METHOD:upx<br>-O2 default (good performance at a reasonable file size) | BP_NATIVE_IMAGE<br>BP_BINARY_COMPRESSION_METHOD:upx<br>-R:MaxHeapSize=48m<br>-O2 default (good performance at a reasonable file size)<br> -gc=parallel | 
| Build Time                                                  | 11m 27s                                                                                                         | 11m 9s                                                                                                                                                 |
| Build Image Size<br>Based on "busybox:stable-glibc"         | 39.2 MB                                                                                                         | 39.4 MB                                                                                                                                                |
| Container Memory Usage                                      | 274 MB (docker run w/o limit)<br>125 MB (docker run --memory=120m --memory-swap=0)                              | 199 MB (docker run w/o limit)<br> 90 MB (docker run --memory=116m --memory-swap=0)                                                                     | 
| CPU Usage Idle/Load                                         | 0% / 2%                                                                                                         | 0.2 % / mostly under 1%, seldom peaks to 18 %                                                                                                          | 
| Startup Time | \>= 1.3s                                                                                                        | \>= 1.1s                                                                                                                                               |
| Endpoint Response Times<br>(GET localhost:8080)             | 4ms to 60ms  (mostly around 5 to 20ms)                                                                          | mostly 1ms - 9 ms, sometimes > 10 ms,<br>seldom peaks over 100ms                                                                                      | 
| Rating                                                      | ★★★☆☆<br>(missing memory optimizations)                                                                        | ★★★★☆<br>(good startup and response times, low memory)                                                                                                 |

The other GraalVM [optimization levels](https://www.graalvm.org/jdk25/reference-manual/native-image/optimizations-and-performance/) -Ob and -Os were not considered:
- The level -Ob produced a binary which was worse than all other variants.
- The level -Os is not available in GraalVM 21, whereas it is documented in the reference manual for GraalVM 25.<br>

## Test Results GraalVM 25
## Test Results
| bootBuildImage native-image                         | + upx compression<br> + busybox runtime<br> + mem optimizations<br> + default optimizations<br> + parallel gc                                                                              | + upx compression<br> + busybox runtime<br> + mem optimizations<br> + optimize for size<br> + parallel gc                                                                        |
|-----------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Build Environment                                   | BP_RUNTIME_CERT_BINDING_DISABLED<br>BP_NATIVE_IMAGE<br>BP_BINARY_COMPRESSION_METHOD:upx<br>-R:MaxHeapSize=48m<br>-O2 default (good performance at a reasonable file size)<br> -gc=parallel | BP_RUNTIME_CERT_BINDING_DISABLED<br>BP_NATIVE_IMAGE<br>BP_BINARY_COMPRESSION_METHOD:upx<br>-R:MaxHeapSize=48m<br>-Os (optimizations, but mainly for image size)<br> -gc=parallel | 
| Build Time                                          | 17m 7s                                                                                                                                                                                     | 16m 28s                                                                                                                                                                          |
| Build Image Size<br>Based on "busybox:stable-glibc" | 33.4 MB<br>(without certificate bindings the image size is significantly smaller)                                                                                                           | 29.2 MB<br>(even smaller because of -Os)                                                                                                                                         |
| Container Memory Usage                              | 144 MB (docker run w/o limit)<br> 61 - 69 MB (docker run --memory=90m --memory-swap=0)                                                                                                     | 124 MB (docker run w/o limit)<br> 66 MB (docker run --memory=90m --memory-swap=0)                                                                                                | 
| CPU Usage Idle/Load                                 | 0.2 % / mostly under 1%, seldom peaks to 18 %                                                                                                                                              | 0.2 % / mostly under 1%, seldom peaks to 18 %                                                                                                                                    | 
| Startup Time                                        | \>= 1.0s                                                                                                                                                                                   | \>= 1.0s                                                                                                                                                                         |
| Endpoint Response Times<br>(GET localhost:8080)     | mostly 1ms - 9 ms, sometimes > 10 ms, seldom peaks over 100ms                                                                                                                              | mostly 1ms - 9 ms, sometimes > 10 ms, seldom peaks over 100ms                                                                                                                    | 
| Rating                                              | ★★★★★<br>(good startup and response times, low memory)                                                                                                                                                             | ★★★★★<br>(good startup and response times, very low memory)                                                                                                                      |

For GraalVM 25 all documented [optimization levels](https://www.graalvm.org/jdk25/reference-manual/native-image/optimizations-and-performance/) are available. Here the option -Os was used:
- The level -Os produces an even smaller image size. At least with this application no negative performance impacts were seen.<br>

Common build image size optimizations
* Paketo's Java Native Image Buildpack uses mostly-static images as default, so a distro-less base image having glibc is sufficient as runtime OS. Each build has been done with "busybox:stable-glibc" as runtime image to optimize the build image size.
* Furthermore each native image has been compressed during build by the [Ultimate Packer for eXecutables](https://upx.github.io/).

### Build and Test Environment
`Kernel: Linux 6.8.0-83-generic`<br>
`CPU: Intel(R) Core(TM) i3-4160T (4) @ 3.10 GHz`<br>
`GPU: Intel 4th Generation Core Processor Family Integrated Graphics Controller @ 1.15 GHz [Integrated]`<br>
`Memory: 12.49 GiB / 15.49 GiB (81%)`<br>
`Swap: 4.55 GiB / 16.76 GiB (27%)`<br>

### Building
Build the native image:
```
./gradlew clean bootBuildImage
```

### Running
Run the image with docker:
```
docker run --rm --memory=116m --memory-swap=0 --name "spring-boot-native-image" -p 8080:8080 spring-boot-native-image:latest
```

## References

### Oracle GraalVM
* [GraalVM Manual](https://www.graalvm.org/latest/reference-manual/)
* [GraalVM - Native Image Build Output](https://github.com/oracle/graal/blob/master/docs/reference-manual/native-image/BuildOutput.md)
* [SBOM  (Software Bill Of Materials) provided in Native Images](https://github.com/oracle/graal/blob/master/docs/security/SBOM.md)
* [GraalVM Community Edition Container Images](https://docs.oracle.com/en/graalvm/jdk/25/docs/docs/getting-started/container-images)
* [Medium - GraalVM News](https://medium.com/graalvm)

### Paketo Buildpacks
* [Paketo Buildpacks Java How To](https://paketo.io/docs/howto/java/)
* [Paketo Buildpacks for GraalVM](https://github.com/paketo-buildpacks/graalvm)
* [Paketo Buildpack for Native Image](https://github.com/paketo-buildpacks/native-image)
* [Buildpacks IO - Pack CLI Tool](https://buildpacks.io/docs/for-platform-operators/how-to/integrate-ci/pack/)

### Tiny Container Images
* [UPX - the Ultimate Packer for eXecutables ](https://upx.github.io/)
* [Busybox - The Swiss Army Knife of Embedded Linux](https://hub.docker.com/_/busybox)

### Gradle Support for Native Images
* [GraalVM Gradle Plugin for GraalVM Native Image](https://graalvm.github.io/native-build-tools/latest/gradle-plugin.html)
* [Spring Boot Packaging OCI Images](https://docs.spring.io/spring-boot/gradle-plugin/packaging-oci-image.html)
* [GraalVM Gradle Plugin provides reachability metadata](https://www.graalvm.org/latest/reference-manual/native-image/guides/use-reachability-metadata-repository-gradle/)

### Sample Projects
* [Native Spring Boot by Alina Yurenko, developer advocate for GraalVM at Oracle](https://github.com/alina-yur/native-spring-boot)
* [Tiny Java Containers by Shaun Smith](https://github.com/graalvm/graalvm-demos/blob/master/native-image/tiny-java-containers/README.md)
* [Paketo Buildpacks Spring Boot Native Image Sample](https://github.com/paketo-buildpacks/samples/blob/main/java/native-image/spring-boot-native-image-gradle/README.md)
