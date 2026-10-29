package com.hrflow.uat;

import com.hrflow.identityorg.auth.setup.UatAccountService;
import java.io.Console;
import java.util.Arrays;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

/** Run through PropertiesLauncher on an interactive private SSH terminal, using the runtime DB login. */
public final class IdentityUatAccountSetup {
    private IdentityUatAccountSetup() {}

    public static void main(String[] args) { System.exit(run(args)); }

    private static int run(String[] args) {
        char[] password = null, confirmation = null;
        try {
            if (args.length == 1 && "--help".equals(args[0])) {
                System.out.println("Private HRFlow UAT account setup\nRequired: --tenant=SLUG --employee=UUID --email=LOGIN --roles=EMPLOYEE,... --time-zone=IANA_ZONE\nUse the configured identity runtime database credentials and an interactive terminal. Passwords are prompted privately twice. Existing accounts are never changed. Apply migrations separately first.");
                return 0;
            }
            var request = UatAccountOptions.parse(args);
            Console console = System.console();
            if (console == null) throw new IllegalArgumentException("An interactive terminal is required; use an SSH terminal. Password arguments and redirected password input are not supported");
            password = console.readPassword("Password: ");
            confirmation = console.readPassword("Confirm password: ");
            if (password == null || confirmation == null || !Arrays.equals(password, confirmation))
                throw new IllegalArgumentException("Passwords did not match; no setup was attempted");
            try (var context = new SpringApplicationBuilder(UatAccountConfiguration.class)
                    .web(WebApplicationType.NONE).bannerMode(Banner.Mode.OFF).run(
                            "--spring.main.web-application-type=none", "--spring.flyway.enabled=false",
                            "--spring.jpa.hibernate.ddl-auto=validate", "--spring.jpa.show-sql=false",
                            "--logging.level.root=WARN", "--logging.level.org.hibernate.orm.jdbc.bind=OFF")) {
                var account = context.getBean(UatAccountService.class).provision(request, password);
                System.out.println("Created account " + account + " linked to employee " + request.employee() + " in tenant " + request.tenant());
            }
            return 0;
        } catch (IllegalArgumentException invalid) {
            System.err.println(invalid.getMessage());
            return 2;
        } catch (RuntimeException unavailable) {
            System.err.println("Setup could not confirm completion. Inspect the account before retrying; check the private service/database configuration.");
            return 1;
        } finally {
            if (password != null) Arrays.fill(password, '\0');
            if (confirmation != null) Arrays.fill(confirmation, '\0');
        }
    }
}
