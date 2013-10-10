package org.scribe.examples;

import java.util.Scanner;

import org.scribe.builder.*;
import org.scribe.builder.api.ADNApi;
import org.scribe.model.*;
import org.scribe.oauth.*;
import org.scribe.utils.OAuthEncoder;

/*
 * An example class to demonstrate and verify the ADN oAuth2 workflow.  Note
 * that you will need to provide an oauth app key and secret token.
 */

public class ADNExample {
	private static final Token EMPTY_TOKEN = null;
	private static final String key = "";
	private static final String secret = "";

	public static void main(String args[]) {
		OAuthService svc = new ServiceBuilder()
			.provider(ADNApi.class)
			.apiKey(key)
			.apiSecret(secret)
			.scope("basic")
			.callback("http://localhost:8000")
			.build();

		Scanner in = new Scanner(System.in);

		System.out.println("=== App.Net oAuth Workflow ===");
		System.out.println();

		System.out.println("Use a web browser and authorize ADN here:");
		System.out.println("\t" + svc.getAuthorizationUrl(EMPTY_TOKEN));
		System.out.println("\nPaste the access token below:");
		System.out.print(">> ");
		Verifier verifier = new Verifier(in.nextLine());
		System.out.println();
		System.out.println("Obtained key");
		Token accessToken = new Token(verifier.getValue(), secret);

		/*
		 * In the client-only model, the access token received is good enough
		 * for use so don't bother with the svc.getAccessToken() call.
		 */

		OAuthRequest req = new OAuthRequest(Verb.GET, "https://alpha-api.app.net/stream/0/users/me");
		svc.signRequest(accessToken, req);
		Response response = req.send();

		System.out.println("Response to our request. Status: " + response.getCode() + ", body:");
		System.out.println(response.getBody());
	}
}