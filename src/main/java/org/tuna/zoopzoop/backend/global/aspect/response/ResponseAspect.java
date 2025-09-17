package org.tuna.zoopzoop.backend.global.aspect.response;

import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.tuna.zoopzoop.backend.global.rsData.RsData;

@Aspect
@Component
public class ResponseAspect {

    private final HttpServletResponse response;

    public ResponseAspect(HttpServletResponse response) {
        this.response = response;
    }

    @Around("""
                execution(public org.tuna.zoopzoop.backend.global.rsData.RsData *(..)) &&
                (
                    within(@org.springframework.stereotype.Controller *) ||
                    within(@org.springframework.web.bind.annotation.RestController *)
                ) &&
                (
                    @annotation(org.springframework.web.bind.annotation.GetMapping) ||
                    @annotation(org.springframework.web.bind.annotation.PostMapping) ||
                    @annotation(org.springframework.web.bind.annotation.PutMapping) ||
                    @annotation(org.springframework.web.bind.annotation.DeleteMapping) ||
                    @annotation(org.springframework.web.bind.annotation.RequestMapping)
                )
            """)
    public Object handleResponse(ProceedingJoinPoint joinPoint) throws Throwable {
        Object proceed = joinPoint.proceed();

        RsData<?> rsData = (RsData<?>) proceed;
        response.setStatus(rsData.statusCode());

        return proceed;
    }
}