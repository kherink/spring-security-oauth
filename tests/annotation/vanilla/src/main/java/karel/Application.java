package karel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Configuration
@ComponentScan
@EnableAutoConfiguration
public class Application {

    private static final String RESOURCE_ID = "blog_resource";

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Configuration
    @EnableAuthorizationServer // [1]
    protected static class OAuth2Config extends AuthorizationServerConfigurerAdapter {

        @Autowired
        private AuthenticationManager authenticationManager;

        @Override // [2]
        public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
            endpoints.authenticationManager(authenticationManager);
        }

        @Override // [3]
        public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
            // @formatter:off
           clients.inMemory()
           .withClient("my-client-with-secret")
			   .authorizedGrantTypes("client_credentials", "password")
			   .authorities("ROLE_CLIENT")
			   .scopes("read")
			   .resourceIds(RESOURCE_ID)
			   .secret("secret");
           // @formatter:on
        }
    }


    @RestController
    public static class ResSvr {

        @RequestMapping("/") //[1]
        public String home() {
            return "Hello World";
        }

        @Configuration
        @EnableResourceServer // [2]
        protected static class ResourceServer extends ResourceServerConfigurerAdapter {

            @Bean
            public UserDetailsService userDetailsService() throws Exception {
                InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
                manager.createUser(User.withUsername("bob").password("abc123").roles("USER").build());
                return manager;
            }

            @Override // [3]
            public void configure(HttpSecurity http) throws Exception {
                // @formatter:off
               http
               // Just for laughs, apply OAuth protection to only 2 resources
               .requestMatchers().antMatchers("/","/admin/beans").and()
               .authorizeRequests()
               .anyRequest().access("#oauth2.hasScope('read')"); //[4]
               // @formatter:on
            }

            @Override
            public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
                resources.resourceId(RESOURCE_ID);
            }

        }
    }
}

