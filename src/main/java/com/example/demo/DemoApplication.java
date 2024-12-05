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
        // Do not hard-code or leak your SSOReady API key in production!
        //
        // In production, instead you should configure a secret SSOREADY_API_KEY
        // environment variable. The SSOReady SDK automatically loads an API key
        // from SSOREADY_API_KEY.
        //
        // This key is hard-coded here for the convenience of logging into a
        // test app, which is hard-coded to run on http://localhost:8080. It's
        // only because of this very specific set of constraints that it's
        // acceptable to hard-code and publicly leak this API key.
        this.ssoready = SSOReady.builder().apiKey("ssoready_sk_3zf23s31evpa68hdxz35ggls5").build();
    }

    // This demo just renders plain old HTML with no client-side JavaScript, and
    // a little bit of pretty TailwindCSS. This is only to keep the demo
    // minimal. SSOReady works with any frontend stack or framework you use.
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

    // This is the page users visit when they click on the "Log out" link in
    // this demo app. It just resets their email cookie.
    //
    // SSOReady doesn't impose any constraints on how your app's sessions work.
    @GetMapping("/logout")
    public RedirectView logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("email", "");
        cookie.setMaxAge(0);

        response.addCookie(cookie);
        return new RedirectView("/");
    }

    // This is the page users visit when they submit the "Log in with SAML" form in
    // this demo app.
    @GetMapping("/saml-redirect")
    public RedirectView samlRedirect(@RequestParam(value = "email") String email) {
        // To start a SAML login, you need to redirect your user to their
        // employer's particular Identity Provider. This is called "initiating"
        // the SAML login.
        //
        // Use `getSamlRedirectUrl` to initiate a SAML login.
        String redirectUrl = this.ssoready.saml().getSamlRedirectUrl(
                GetSamlRedirectUrlRequest
                        .builder()
                        // organizationExternalId is how you tell SSOReady which
                        // company's identity provider you want to redirect to.
                        //
                        // In this demo, we identify companies using their
                        // domain. This code converts "john.doe@example.com"
                        // into "example.com".
                        .organizationExternalId(email.split("@")[1])
                        .build()
        ).getRedirectUrl().orElseThrow();

        return new RedirectView(redirectUrl);
    }

    // This is the page SSOReady redirects your users to when they've
    // successfully logged in with SAML.
    @GetMapping("/ssoready-callback")
    public RedirectView ssoreadyCallback(HttpServletResponse response, @RequestParam(value = "saml_access_code") String samlAccessCode) {
        // SSOReady gives you a one-time SAML access code under
        // ?saml_access_code=saml_access_code_... in the callback URL's query
        // parameters.
        //
        // You redeem that SAML access code using `redeemSamlAccessCode`, which
        // gives you back the user's email address. Then, it's your job to log
        // the user in as that email.
        String email = this.ssoready.saml().redeemSamlAccessCode(
                RedeemSamlAccessCodeRequest
                        .builder()
                        .samlAccessCode(samlAccessCode)
                        .build()
        ).getEmail().orElseThrow();

        // SSOReady works with any stack or session technology you use. In this
        // demo app, we just directly use HttpServletResponse.
        Cookie cookie = new Cookie("email", email);
        cookie.setMaxAge(3600);
        response.addCookie(cookie);

        // Redirect back to the demo app homepage.
        return new RedirectView("/");
    }
}
