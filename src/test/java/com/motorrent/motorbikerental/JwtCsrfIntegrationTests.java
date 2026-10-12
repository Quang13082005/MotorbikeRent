package com.motorrent.motorbikerental;
import com.motorrent.motorbikerental.entity.*;
import com.motorrent.motorbikerental.repository.*;
import com.motorrent.motorbikerental.enums.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;
import java.time.LocalDateTime;
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:jwt_csrf;MODE=MySQL;NON_KEYWORDS=YEAR;DB_CLOSE_DELAY=-1")
class JwtCsrfIntegrationTests {
 @Autowired WebApplicationContext context;
 MockMvc mvc; User customer;
 @BeforeEach void setup(){
  mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  customer=context.getBean(UserRepository.class).saveAndFlush(User.builder().username("alice").email("alice@example.test").password("unused").firstName("An").lastName("Test").role(UserRole.CUSTOMER).build());
 }
 @Test void jwtResourceRequestsDoNotInvalidateProfileCsrf() throws Exception {
  var token=context.getBean(com.motorrent.motorbikerental.security.JwtUtil.class).generateToken(customer.getId(),customer.getUsername(),"CUSTOMER");
  context.getBean(TokenAuthenticationRepository.class).save(TokenAuthentication.builder().user(customer).tokenValue(token).isUsed(true).expiresAt(LocalDateTime.now().plusHours(1)).build());
  var authCookie=new jakarta.servlet.http.Cookie("access_token",token);
  var page=mvc.perform(get("/customer/profile").cookie(authCookie)).andExpect(status().isOk()).andReturn().getResponse();
  var csrfCookie=page.getCookie("XSRF-TOKEN");assertThat(csrfCookie).isNotNull();
  for(String asset:java.util.List.of("/js/customer.js","/css/customer-workspace.css","/images/motorbike-placeholder.svg")) {
   var response=mvc.perform(get(asset).cookie(authCookie,csrfCookie)).andExpect(status().isOk()).andReturn().getResponse();
   assertThat(response.getCookie("XSRF-TOKEN")).as("Assets must not rotate CSRF").isNull();
  }
  mvc.perform(put("/api/account").cookie(authCookie,csrfCookie).header("X-XSRF-TOKEN",csrfCookie.getValue()).contentType("application/json")
   .content("{\"firstName\":\"An\",\"lastName\":\"Nguyễn\",\"email\":\"alice@example.test\",\"phone\":\"0905123456\"}"))
   .andExpect(status().isOk());
 }

}
