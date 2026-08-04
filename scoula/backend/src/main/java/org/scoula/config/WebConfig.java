package org.scoula.config;


import org.scoula.security.config.SecurityConfig;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletRegistration;
import java.io.File;

public class WebConfig extends AbstractAnnotationConfigDispatcherServletInitializer {

    final long MAX_FILE_SIZE = 1024 * 1024 * 10L;
    final long MAX_REQUEST_SIZE = 1024 * 1024 * 20L;
    final int FILE_SIZE_THRESHOLD = 1024 * 1024 * 5;

    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class[]{RootConfig.class, SecurityConfig.class};
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class[]{ServletConfig.class};
    }

    // 스프링의 FrontController인 DispatcherServlet이 담당할 Url 매핑 패턴, / : 모든 요청에 대해 매핑
    @Override
    protected String[] getServletMappings() {
        return new String[]{"/"};
    }

    // POST body 문자 인코딩 필터 설정 - UTF-8 설정
//    protected Filter[] getServletFilters() {
//        CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();
//
//        characterEncodingFilter.setEncoding("UTF-8");
//        characterEncodingFilter.setForceEncoding(true);
//
//        return new Filter[]{characterEncodingFilter};
//    }

    @Override
    protected void customizeRegistration(ServletRegistration.Dynamic registration) {
        registration.setInitParameter("throwExceptionIfNoHandlerFound", "true");
        File uploadDirectory = new File(System.getProperty("java.io.tmpdir"), "scoula/upload");
        if (!uploadDirectory.exists() && !uploadDirectory.mkdirs()) {
            throw new IllegalStateException("임시 업로드 폴더를 만들 수 없습니다: " + uploadDirectory);
        }

        MultipartConfigElement multipartConfig =
                new MultipartConfigElement(
                        uploadDirectory.getAbsolutePath(),   // 운영체제의 임시 디렉토리 아래에 생성
                        MAX_FILE_SIZE,    // 업로드 가능한 파일 하나의 최대 크기
                        MAX_REQUEST_SIZE,    // 업로드 가능한 전체 최대 크기(여러 파일 업로드 하는 경우)
                        FILE_SIZE_THRESHOLD        // 메모리 파일의 최대 크기(이보다 작으면 실제 메모리에서만 작업)
                );
        registration.setMultipartConfig(multipartConfig);
    }


}
