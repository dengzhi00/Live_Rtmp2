#设置变量 cflags
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -mfloat-abi=softfp -mfpu=vfpv3-d16 -O2 -DSYS=posix -DNO_CRYPTO"  )

#设置源文件变量
file(GLOB rtmp_source *.c)

#编译 librtmp.a
add_library( #模块名
             rtmp
            # 动态库
             STATIC
            #源文件
            ${rtmp_source} )