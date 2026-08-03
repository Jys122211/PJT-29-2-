package org.scoula.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@EnableWebMvc
@ComponentScan(basePackages = {
        "org.scoula.exception",
        "org.scoula.controller",
        "org.scoula.board.controller",
        "org.scoula.member.controller",
        "org.scoula.profitLoss.controller",
})
public class ServletConfig implements WebMvcConfigurer {

    /**
     * Vue가 담당하는 화면 URL을 새로고침해도 404가 발생하지 않도록
     * 모든 화면 요청을 빌드된 Vue index.html로 전달한다.
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        String spaView = "forward:/resources/index.html";
        String[] spaRoutes = {
                "/",
                "/login",
                "/signup",
                "/signup/complete",
                "/dashboard",
                "/deposits/count"
        };

        for (String route : spaRoutes) {
            registry.addViewController(route).setViewName(spaView);
        }
    }


    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler("/resources/**")     // url이 /resources/로 시작하는 모든 경로
                .addResourceLocations("/resources/");    // webapp/resources/경로로 매핑

        registry.addResourceHandler("/assets/**")
                .addResourceLocations("/resources/assets/");


    }


    //	Servlet 3.0 파일 업로드 사용시
    @Bean
    public MultipartResolver multipartResolver() {
        StandardServletMultipartResolver resolver = new StandardServletMultipartResolver();
        return resolver;
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        converters.add(0, new MappingJackson2HttpMessageConverter(mapper));
    }


}
