
#版本号
cmake_minimum_required(VERSION 3.4.1)
#加载其他的cmake文件
add_subdirectory(src/main/cpp/librtmp)

set(my_lib_path ${CMAKE_SOURCE_DIR}/src/main/cpp/lib)
#导入编译好的静态库
add_library(avutil STATIC IMPORTED)
#设置导入的路径
set_target_properties(avutil PROPERTIES IMPORTED_LOCATION ${my_lib_path}/libavutil.a)
#导入编译好的静态库
add_library(swscale STATIC IMPORTED)
#设置导入的路径
set_target_properties(swscale PROPERTIES IMPORTED_LOCATION ${my_lib_path}/libswscale.a)
#导入编译好的静态库
add_library(x264 STATIC IMPORTED)
#设置导入的路径
set_target_properties(x264 PROPERTIES IMPORTED_LOCATION ${my_lib_path}/libx264.a)
#导入编译好的静态库
add_library(faac STATIC IMPORTED)
#设置导入的路径
set_target_properties(faac PROPERTIES IMPORTED_LOCATION ${my_lib_path}/libfaac.a)

include_directories(src/main/cpp)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fPIC -fpermissive -mfloat-abi=softfp -mfpu=vfpv3-d16  -D__STDC_CONSTANT_MACROS"  )
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")


file(GLOB my_source_path src/main/cpp/*.cpp src/main/cpp/*.c)
#添加模块
add_library( #模块名
             Dvr
            # 动态库
             SHARED
            #源文件
             ${my_source_path} )


#查找系统库
find_library(
             #变量名
              log-lib
            #库名
              log )

find_library(
             #变量名
              z-lib
            #库名
              z )
              find_library(
                           #变量名
                            jnigraphics-lib
                          #库名
                            jnigraphics )


#需要链接或者编译的库
target_link_libraries(
                       Dvr
                       rtmp
                       swscale
                       avutil
                       x264
                       faac
                       ${log-lib}
                        ${z-lib}
                        ${jnigraphics-lib})
