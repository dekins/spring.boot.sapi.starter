package com.github.xiaour.api_scanner.filter;

import com.github.xiaour.api_scanner.dto.ApiField;
import com.github.xiaour.api_scanner.dto.ApiInfo;
import com.github.xiaour.api_scanner.exception.SimpleApiException;
import com.github.xiaour.api_scanner.logging.Log;
import com.github.xiaour.api_scanner.logging.LogFactory;
import com.github.xiaour.api_scanner.util.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.File;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;



/**
 * @Author: Xiaour
 * @Description:
 * @Date: 2018/5/30 15:02
 */
@Component
public class ApiInit {

    private final static Log LOG = LogFactory.getLog(ApiInit.class);


    @Autowired
    private ApiProperties properties;

    protected static String simpleApiJson;

    @PostConstruct
    public void init(){

        LOG.debug("The springboot sapi init.");

        Set<Class> classes= new HashSet<>();
        try {

            if(properties.getPack()==null||properties.getPack().length<=0){
                LOG.error("Not config Springboot SimpleApi.");
                throw new SimpleApiException("Not config Springboot SimpleApi.");
            }
            for(String packageName:properties.getPack()){
                    classes.addAll(getClassName(packageName));
            }

            List<ApiInfo> list=new ArrayList<>();

            for(Class c:classes){


                RequestMapping requestMapping= (RequestMapping) c.getAnnotation(RequestMapping.class);

                if(requestMapping==null){
                    list.addAll(getReflectAllMethod(c,new String[]{""}));
                }else {
                    list.addAll(getReflectAllMethod(c, requestMapping.value()));
                }
            }

            simpleApiJson=JsonUtil.collectionJsonUtil(list);
            LOG.debug("Springboot sapi : open link view the API page on http://127.0.0.1:{port}/{context-path}/sapi");
            LOG.info("Springboot sapi : open link view the API page on http://127.0.0.1:{port}/{context-path}/sapi");

        } catch (Exception e) {
           LOG.error("Sapi init exception:",e);
        }
    }

    private static Set<Class> getClassName(String filePath) throws ClassNotFoundException {
        Set<Class> classes= new HashSet<>();
        filePath = ClassLoader.getSystemResource("").getPath() + filePath.replace(".", "/");
        File file = new File(filePath);
        File[] childFiles = file.listFiles();
        for (File childFile : childFiles) {
            String childFilePath = childFile.getPath();
            childFilePath = childFilePath.substring(childFilePath.indexOf("/classes") + 9,childFilePath.length());
            childFilePath=childFilePath.replaceAll(".class","");
            childFilePath=childFilePath.replaceAll("/",".");
            classes.add(Class.forName(childFilePath));
        }

        return classes;
    }




    private List<ApiInfo> getReflectAllMethod( Class <?> mLocalClass,String [] routes){
        ParameterNameDiscoverer pnd = new LocalVariableTableParameterNameDiscoverer();
        List<ApiInfo> list= new ArrayList<>();

        try {

            do{
                Method methods[] = mLocalClass.getDeclaredMethods(); // 取得全部的方法
                for (Method method:methods) {

                    for(String route:routes){
                        String mod = Modifier.toString(method.getModifiers());
                        // 取得方法名称
                        String metName = method.getName();
                        ApiInfo apiInfo= new ApiInfo();

                        if(mod.equals("public")&&!metName.equals("toString")&&!metName.equals("equals")) {
                            RequestMethod[] me=method.getAnnotation(RequestMapping.class).method();
                            for(RequestMethod rm:me){
                                apiInfo.setRequestType(apiInfo.getRequestType()!=""?apiInfo.getRequestType()+","+rm:rm.name());
                            }
                            apiInfo.setUrl(route+"/"+metName);
                            Class<?> paramsTypes[] = method.getParameterTypes(); // 得到全部的参数类型
                            String[] paramNames = pnd.getParameterNames(method);//返回的就是方法中的参数名列表了

                            //获取字段
                            int length=paramsTypes.length;
                            List<ApiField> apiFields=new ArrayList<>(length);
                            for(int i=0;i<length;i++){
                                String type=paramsTypes[i].toString().substring(paramsTypes[i].toString().lastIndexOf(".")+1,paramsTypes[i].toString().length());
                                ApiField apiField= new ApiField();
                                apiField.setName(paramNames[i]);
                                apiField.setType(type);

                                apiFields.add(apiField);
                            }
                            apiInfo.setFieldList(apiFields);
                            list.add(apiInfo);
                        }
                    }
                }

                mLocalClass=mLocalClass.getSuperclass();

            }while(mLocalClass!=null);
        } catch (Exception e) {
            LOG.error("Sapi init exception:",e);
        }
        return list;
    }
}