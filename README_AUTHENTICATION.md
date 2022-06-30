# CACAO

It’s possible to use CACAO with an external OpenId/OAuth authentication provider, such as Google Authentication and Microsoft Azure Authentication. 
By doing this, it’s possible to use user accounts stored externally, without the need to manage user credentials inside CACAO. 
In this way, CACAO does not have to deal with user passwords, but simply relies on the service provided by third parties for verifying user identity. 
It’s not mandatory to do this kind of configuration since CACAO provides its own authentication methods. 
But it’s something that can add ease of use to the application. 

The steps necessary to configure this kind of service may vary from one provider to another. 
Each authentication provider may require you to sign legal terms of responsibility related to using their service (the authentication procedure of user accounts maintained under their domain) with an external application (the installation of CACAO at your own domain). 
There will probably be a ‘contract’ between you (or your company) and the authentication provider related to using this service. 
It’s out of the scope of this manual to go through all the steps related to each authentication provider, but it will be shown how to do it with two of them for illustration purpose. 
You will also have to inform the URL’s related to your installation of CACAO, so it needs to be done before. 
Eventually they will all present at some point a set of registration keys that must be configured inside CACAO. 

Usually these include:

* An identification of the application that is being registered (something usually called ‘client id’). Usually it’s a sequence of characters, including letters, numbers and symbols.
* A randomly generated secret for this kind of access (something usually called ‘client secret’). Usually, it’s also a sequence of characters.

Once you have obtained all the registration keys related to the authentication service, you may configure CACAO using some specific properties inside the file ***app_config_web***.

Usually, the authentication provider will not charge your organization for this kind of service. But it’s important to check with your legal department if this actually applies to your situation.

Some authentication providers may also inform about an expiration date for using these application credentials (perhaps 2 years or more), so you must keep it in mind. 
Once it has expired, you will probably have to generate a new secret and update the CACAO configuration with it.

The configuration settings inside CACAO ***app_config_web*** configuration file will look something like this:

    spring.security.oauth2.client.registration.{provider}.client-id={client id}
    spring.security.oauth2.client.registration.{provider}.client-secret={client secret}

    
The ***{provider}*** part of the configuration name will be something related to the provider’s name (e.g.: ‘google’, ‘facebook’, ‘azure’). The ***{client id}*** and the ***{client secret}*** are provided to you as part of the application registration steps at the authentication provider. 

After changing the ***app_config_web*** configuration file, you have to restart the CACAO web component using this command line:
    
    docker-compose restart web
    
After the application has restarted, the login screen of CACAO will show additional entries for logging in using these different services. 

It’s possible to add configurations related to more than one application provider to be used by CACAO. For example, you may have configurations for both Google Authentication and Microsoft Azure Authentication. Additional login entries will be displayed for each one of them at CACAO login screen. 

Just to illustrate, the following boxes summarizes some of the steps required for two common authentication providers. Please note that these instructions are based on the provider’s web pages and may be outdated by the time you perform these steps.

___

## How to Setup Google Authentication (Login with Google)

- Create a Google account and create a Project (see https://cloud.google.com/resource-manager/docs/creating-managing-projects)
- Configure a 'Consent Screen' (https://console.cloud.google.com/apis/credentials/consent)
  - When prompted to choose between 'Internal' or 'Public' application, choose 'Public' in order to allow users outside your organization to access the website
  - Inform the product name (e.g. CACAO) and e-mail
  - Upload the application logo (image file with size lesser than 1 MB)
  - For start page, inform the URI within the domain name (e.g.: https://cacaoidb.duckdns.org/)
  - For Privacy Policy page, inform the following URI (change only domain name if necessary, keep the rest of this URI): https://cacaoidb.duckdns.org/privacy
  - For Terms of Use page, inform the following URI (change only domain name if necessary, keep the rest of this URI): https://cacaoidb.duckdns.org/terms
  - For Authorized Domain Names, inform the domain name (e.g. cacaoidb.duckdns.org)
  - For developer contact, inform your e-mail
  - Click 'Add Scope'
  - Choose the following scopes from the list of scopes:
    - .../auth/userinfo.email		(See your primary Google Account email address)
    - .../auth/userinfo.profile		(See your personal info, including any personal info you've made publicly available)
    - 	openid						(Associate you with your personal info on Google)
  - Add some initial 'test users' for testing purposes
- Create OAuth2 credentials in Credentials Page (for more information, see https://developers.google.com/identity/protocols/oauth2/openid-connect and https://console.developers.google.com/apis/credentials)
  - Go to this link and create a new OAuth2 token: https://console.developers.google.com/apis/credentials
  - Choose a name for this OAuth token (e.g. CACAO)
  - For Javascript Authorized Sources, informs the following URI (change only domain name if necessary, keep the rest of this URI): https://cacaoidb.duckdns.org
  - If also using for development environment, also include the following URL for authorized JavaScript source: `https://127.0.0.1:8888`
  - Set a redirect UI following this pattern (change only domain name if necessary, keep the rest of this URI): https://cacaoidb.duckdns.org/login/oauth2/code/google
  - If also using for development environment, also include the following URL for authorized redirections: `https://127.0.0.1:8888/login/oauth2/code/google`
  - Take note of these information and keep secret (do not publish anywhere):
    - Application ID (client)
    - Secret
  - You must inform these information in the configuration file 'app_config_web', at the deployment environment, accessible only by root user
    - spring.security.oauth2.client.registration.google.client-id=<copy here you application ID from Google>
    - spring.security.oauth2.client.registration.google.client-secret=<copy here your secret from Google>
- Publish the application (from 'Testing' to 'Production')
  - Access the 'Consent Screen' configuration page (https://console.cloud.google.com/apis/credentials/consent)
  - Press the 'Publish Application' button
  
___
  
## How to Setup Microsoft Azure Authentication

- Create an account at Microsoft Azure and create a Tenant (see https://docs.microsoft.com/pt-br/azure/active-directory/fundamentals/active-directory-access-create-new-tenant)
  - The page for tenant creation is here: https://portal.azure.com/#create/hub (at this page, search for 'Azure Active Directory')
  - You will provide an 'organization name', an initial 'domain name' (the application website) and your country
- Create OAuth2 credentials registering new app (see https://docs.microsoft.com/pt-br/azure/active-directory/develop/quickstart-register-app)
  - The page for application creation is here: https://go.microsoft.com/fwlink/?linkid=2083908
  - For 'type of account', choose 'Accounts in any tenant (multitenant) including personal accounts (e.g. Skype, Xbox, etc.)
  - Go to this link and provide additional information. When asked, inform the following endpoint (change only domain name if necessary, keep the rest of this URI): https://cacaoidb.duckdns.org/login/oauth2/code/azure
  - You may need to include additional callback endpoint if using for development environment: `https://127.0.0.1:8888/login/oauth2/code/azure`
- Take note of these information and keep secret (do not publish anywhere):
  - Application ID (client)
  - Active Directory ID (tenant)
  - Object ID
  - Secret (valid for one year or two year)
  
    IMPORTANT: The ‘secret’ you need to configure in CACAO is ***NOT*** the ***secret ID***, but the ***secret value*** that will be presented to you only once by Microsoft (it’s a one-time view only, you’ll have to create another one if you miss it).
  
- Consent permission to application 
  - Go to panel 'Permissions of API'
  - Click que button/check mark 'Consent'
- Include OpenId claims (email, family_name and given_name) at Token Configuration   
- You must inform these information in the configuration file 'app_config_web', at the deployment environment, accessible only by root user
  - azure.activedirectory.tenant-id=<copy here you tenant ID from Microsoft>
  - spring.security.oauth2.client.registration.azure.client-id=<copy here your client ID from Microsoft>
  - spring.security.oauth2.client.registration.azure.client-secret=<copy here you secret from Microsoft>
  - azure.activedirectory.user-group.allowed-groups=Users
  - spring.cloud.azure.telemetry.enable=false
  - azure.mediaservices.allow-telemetry=false
  - azure.activedirectory.allow-telemetry=false
    