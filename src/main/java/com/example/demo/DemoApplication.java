package com.example.demo;

import com.ssoready.api.SSOReady;
import com.ssoready.api.resources.saml.requests.GetSamlRedirectUrlRequest;
import com.ssoready.api.resources.saml.requests.RedeemSamlAccessCodeRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Optional;

@SpringBootApplication
@RestController
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    private SSOReady ssoready;

    public DemoApplication() {
        this.ssoready = SSOReady.builder().apiKey("ssoready_sk_3zf23s31evpa68hdxz35ggls5").build();
    }

    @GetMapping(path = "/", produces = "text/html")
    public String index(@CookieValue("email") Optional<String> email) {
        return """
                <html>
                  <head>
                    <title>SAML Demo App using SSOReady</title>
                    <script src="https://cdn.tailwindcss.com"></script>
                  </head>
                  <body>
                    <main class="grid min-h-full place-items-center py-32 px-8">
                      <div class="text-center">
                        <h1 class="mt-4 text-balance text-5xl font-semibold tracking-tight text-gray-900 sm:text-7xl">
                          <!-- this parameter gets populated from /ssoready-callback -->
                          Hello, %s!
                        </h1>
                        <p class="mt-6 text-pretty text-lg font-medium text-gray-500 sm:text-xl/8">
                          This is a SAML demo app, built using SSOReady.
                        </p>
                
                        <!-- submitting this form makes the user's browser do a GET /saml-redirect?email=... -->
                        <form method="get" action="/saml-redirect" class="mt-10 max-w-lg mx-auto">
                          <div class="flex gap-x-4 items-center">
                            <label for="email-address" class="sr-only">Email address</label>
                            <input id="email-address" name="email" class="min-w-0 flex-auto rounded-md border-0 px-3.5 py-2 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-indigo-600 sm:text-sm sm:leading-6" value="john.doe@example.com" placeholder="john.doe@example.com">
                            <button type="submit" class="flex-none rounded-md bg-indigo-600 px-3.5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-indigo-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600">
                              Log in with SAML
                            </button>
                            <a href="/logout" class="px-3.5 py-2.5 text-sm font-semibold text-gray-900">
                              Sign out
                            </a>
                          </div>
                          <p class="mt-4 text-sm leading-6 text-gray-900">
                            (Try any @example.com or @example.org email address.)
                          </p>
                        </form>
                      </div>
                    </main>
                  </body>
                </html>
                """.formatted(email.orElse("logged-out user"));
    }

    @GetMapping("/logout")
    public RedirectView logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("email", "");
        cookie.setMaxAge(0);

        response.addCookie(cookie);
        return new RedirectView("/");
    }

    @GetMapping("/saml-redirect")
    public RedirectView samlRedirect(@RequestParam(value = "email") String email) {
        String redirectUrl = this.ssoready.saml().getSamlRedirectUrl(
                GetSamlRedirectUrlRequest
                        .builder()
                        .organizationExternalId(email.split("@")[1])
                        .build()
        ).getRedirectUrl().orElseThrow();

        return new RedirectView(redirectUrl);
    }

    @GetMapping("/ssoready-callback")
    public RedirectView ssoreadyCallback(HttpServletResponse response, @RequestParam(value = "saml_access_code") String samlAccessCode) {
        String email = this.ssoready.saml().redeemSamlAccessCode(
                RedeemSamlAccessCodeRequest
                        .builder()
                        .samlAccessCode(samlAccessCode)
                        .build()
        ).getEmail().orElseThrow();

        Cookie cookie = new Cookie("email", email);
        cookie.setMaxAge(3600);
        response.addCookie(cookie);

        return new RedirectView("/");
    }
}
