-- SnowX IAM PostgreSQL schema baseline, migrated from MaxKey v4.2.0.
-- MaxKey v4.2.0 PostgreSQL ?????
-- ? sql/v4.2.0/maxkey.sql ?????? MySQL ???????
-- ??? UTF-8 ????????
CREATE TABLE mxk_access (

  id varchar(45) NOT NULL,
  groupid varchar(45) NOT NULL,
  appid varchar(45) NOT NULL,
  createddate timestamp DEFAULT CURRENT_TIMESTAMP,
  instid varchar(45) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT mxk_access_groupid_appid UNIQUE (groupid,appid)
);
CREATE TABLE mxk_accounts (

  id varchar(45) NOT NULL,
  userid varchar(45) DEFAULT NULL,
  username varchar(45) DEFAULT NULL,
  displayname varchar(45) DEFAULT NULL,
  strategyname varchar(200) DEFAULT NULL,
  strategyid varchar(45) DEFAULT NULL,
  appid varchar(45) DEFAULT NULL,
  appname varchar(100) DEFAULT NULL,
  relatedusername varchar(200) DEFAULT NULL,
  relatedpassword varchar(500) DEFAULT NULL,
  createtype varchar(45) DEFAULT E'automatic',
  status int DEFAULT NULL,
  createdby varchar(45) DEFAULT NULL,
  createddate timestamp DEFAULT CURRENT_TIMESTAMP,
  modifiedby varchar(45) DEFAULT NULL,
  modifieddate timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  instid varchar(45) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT mxk_accounts_unique_user_account UNIQUE (username,appid,relatedusername,userid)
);
CREATE TABLE mxk_apps (

  id varchar(45) NOT NULL,
  appname varchar(300) NOT NULL,
  loginurl varchar(300) NOT NULL,
  category varchar(45) DEFAULT NULL,
  secret varchar(500) DEFAULT NULL,
  protocol varchar(300) DEFAULT NULL,
  icon bytea,
  status integer DEFAULT NULL,
  createdby varchar(45) DEFAULT NULL,
  createddate timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  modifiedby varchar(45) DEFAULT NULL,
  modifieddate timestamp DEFAULT NULL,
  description varchar(400) DEFAULT NULL,
  vendor varchar(45) DEFAULT NULL,
  vendorurl varchar(200) DEFAULT NULL,
  credential varchar(45) DEFAULT E'none',
  sharedusername varchar(100) DEFAULT NULL,
  sharedpassword varchar(500) DEFAULT NULL,
  systemuserattr varchar(45) DEFAULT NULL,
  isextendattr varchar(4) DEFAULT NULL,
  extendattr varchar(4000) DEFAULT NULL,
  sortindex integer DEFAULT E'0',
  issignature integer DEFAULT E'0',
  visible integer DEFAULT E'0',
  isadapter integer DEFAULT E'0',
  adapterid varchar(45) DEFAULT NULL,
  adaptername varchar(100) DEFAULT NULL,
  adapter varchar(500) DEFAULT NULL,
  principal varchar(45) DEFAULT NULL,
  credentials varchar(500) DEFAULT NULL,
  userpropertys varchar(4000) DEFAULT NULL,
  inducer varchar(50) DEFAULT E'IDP',
  logouturl varchar(300) DEFAULT NULL,
  logouttype int DEFAULT NULL,
  instid varchar(45) NOT NULL,
  frequently varchar(45) DEFAULT E'no',
  resourcemgt varchar(10) DEFAULT E'n',
  openapiright varchar(10) DEFAULT E'read',
  PRIMARY KEY (id)
);
CREATE TABLE mxk_apps_adapters (

  id varchar(50) NOT NULL,
  name varchar(100) DEFAULT NULL,
  protocol varchar(300) DEFAULT NULL,
  adapter varchar(500) DEFAULT NULL,
  sortindex int DEFAULT NULL,
  createdby varchar(45) DEFAULT NULL,
  createddate timestamp DEFAULT CURRENT_TIMESTAMP,
  modifiedby varchar(45) DEFAULT NULL,
  modifieddate timestamp DEFAULT NULL,
  description varchar(500) DEFAULT NULL,
  PRIMARY KEY (id)
);
CREATE TABLE mxk_apps_cas_details (

  id varchar(45) NOT NULL,
  service varchar(400) NOT NULL,
  callbackurl varchar(400) NOT NULL,
  expires int DEFAULT NULL,
  instid varchar(45) NOT NULL,
  casuser varchar(45) DEFAULT NULL,
  PRIMARY KEY (id)
);
CREATE TABLE mxk_apps_form_based_details (

  id varchar(45) NOT NULL,
  usernamemapping varchar(45) DEFAULT NULL,
  passwordmapping varchar(45) DEFAULT NULL,
  redirecturi varchar(400) DEFAULT NULL,
  authorizeview varchar(100) DEFAULT NULL,
  passwordalgorithm varchar(45) DEFAULT NULL,
  instid varchar(45) NOT NULL,
  PRIMARY KEY (id)
);
CREATE TABLE mxk_apps_jwt_details (

  id varchar(45) NOT NULL,
  issuer varchar(200) DEFAULT NULL,
  subject varchar(100) DEFAULT NULL,
  audience varchar(200) DEFAULT NULL,
  algorithmkey text NOT NULL,
  algorithm varchar(45) NOT NULL,
  encryptionmethod varchar(45) DEFAULT NULL,
  signature varchar(45) DEFAULT NULL,
  signaturekey text,
  expires integer DEFAULT E'0',
  redirecturi varchar(400) NOT NULL,
  jwtname varchar(45) DEFAULT NULL,
  tokentype varchar(20) DEFAULT NULL,
  instid varchar(45) NOT NULL,
  PRIMARY KEY (id)
);
CREATE TABLE mxk_apps_oauth_client_details (

  client_id varchar(45) NOT NULL,
  resource_ids varchar(256) DEFAULT NULL,
  client_secret varchar(500) DEFAULT NULL,
  scope varchar(256) DEFAULT NULL,
  authorized_grant_types varchar(256) DEFAULT NULL,
  web_server_redirect_uri varchar(512) DEFAULT NULL,
  authorities varchar(256) DEFAULT NULL,
  access_token_validity integer DEFAULT NULL,
  refresh_token_validity integer DEFAULT NULL,
  additional_information varchar(4096) DEFAULT NULL,
  approvalprompt varchar(45) DEFAULT E'force',
  autoapprove varchar(256) DEFAULT NULL,
  pkce varchar(45) DEFAULT NULL,
  protocol varchar(45) DEFAULT NULL,
  instid varchar(45) NOT NULL,
  issuer varchar(200) DEFAULT NULL,
  audience varchar(200) DEFAULT NULL,
  algorithmkey text,
  algorithm varchar(45) DEFAULT NULL,
  encryptionmethod varchar(45) DEFAULT NULL,
  signature varchar(45) DEFAULT NULL,
  signaturekey text,
  userinforesponse varchar(45) DEFAULT E'Normal',
  subject varchar(45) DEFAULT E'username',
  PRIMARY KEY (client_id)
);
CREATE TABLE mxk_apps_saml_v20_details (

  id varchar(45) NOT NULL,
  certissuer varchar(200) DEFAULT NULL,
  certsubject varchar(200) DEFAULT NULL,
  certexpiration varchar(100) DEFAULT NULL,
  keystore bytea,
  spacsurl varchar(200) NOT NULL,
  issuer varchar(300) DEFAULT NULL,
  entityid varchar(300) DEFAULT NULL,
  validityinterval integer DEFAULT NULL,
  nameidformat varchar(45) DEFAULT NULL,
  nameidconvert varchar(45) DEFAULT NULL,
  nameidsuffix varchar(150) DEFAULT NULL,
  audience varchar(300) DEFAULT NULL,
  encrypted varchar(45) DEFAULT NULL,
  binding varchar(45) DEFAULT NULL,
  signature varchar(45) DEFAULT NULL,
  digestmethod varchar(45) DEFAULT NULL,
  metaurl varchar(500) DEFAULT NULL,
  instid varchar(45) NOT NULL,
  PRIMARY KEY (id)
);
CREATE TABLE mxk_apps_token_based_details (

  id varchar(45) NOT NULL,
  algorithmkey varchar(500) NOT NULL,
  algorithm varchar(45) NOT NULL,
  expires integer DEFAULT E'0',
  redirecturi varchar(400) NOT NULL,
  cookiename varchar(45) DEFAULT NULL,
  tokentype varchar(20) DEFAULT NULL,
  instid varchar(45) NOT NULL,
  PRIMARY KEY (id)
);
CREATE TABLE mxk_cnf_email_senders (

  id varchar(50) NOT NULL,
  smtphost varchar(45) DEFAULT NULL,
  port int DEFAULT NULL,
  account varchar(45) DEFAULT NULL,
  credentials varchar(500) DEFAULT NULL,
  sslswitch int DEFAULT NULL,
  sender varchar(45) DEFAULT NULL,
  protocol varchar(45) DEFAULT NULL,
  encoding varchar(45) DEFAULT NULL,
  status int DEFAULT NULL,
  instid varchar(45) DEFAULT NULL,
  description varchar(45) DEFAULT NULL,
  createdby varchar(45) DEFAULT NULL,
  createddate timestamp DEFAULT CURRENT_TIMESTAMP,
  modifiedby varchar(45) DEFAULT NULL,
  modifieddate timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);
CREATE TABLE mxk_cnf_ldap_context (

  id varchar(50) NOT NULL,
  product varchar(45) DEFAULT NULL,
  sslswitch varchar(45) DEFAULT NULL,
  providerurl varchar(200) DEFAULT NULL,
  principal varchar(100) DEFAULT NULL,
  credentials varchar(500) DEFAULT NULL,
  basedn varchar(500) DEFAULT NULL,
  filters varchar(500) DEFAULT NULL,
  truststore varchar(500) DEFAULT NULL,
  truststorepassword varchar(100) DEFAULT NULL,
  msaddomain varchar(100) DEFAULT NULL,
  accountmapping varchar(45) DEFAULT E'YES',
  status int DEFAULT NULL,
  description varchar(500) DEFAULT NULL,
  instid varchar(45) DEFAULT NULL,
  createdby varchar(45) DEFAULT NULL,
  createddate timestamp DEFAULT CURRENT_TIMESTAMP,
  modifiedby varchar(45) DEFAULT NULL,
  modifieddate timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);
CREATE TABLE mxk_cnf_password_policy (

  id varchar(45) NOT NULL,
  minlength integer DEFAULT E'0',
  maxlength integer DEFAULT E'0',
  lowercase integer DEFAULT E'0',
  uppercase integer DEFAULT E'0',
  digits integer DEFAULT E'0',
  specialchar integer DEFAULT E'0',
  attempts integer DEFAULT E'0',
  duration integer DEFAULT E'0',
  expiration integer DEFAULT E'0',
  username integer DEFAULT E'0',
  history integer DEFAULT E'0',
  dictionary integer DEFAULT NULL,
  alphabetical integer DEFAULT NULL,
  numerical integer DEFAULT NULL,
  qwerty integer DEFAULT NULL,
  occurances integer DEFAULT NULL,
  createdby varchar(45) DEFAULT NULL,
  createddate timestamp DEFAULT CURRENT_TIMESTAMP,
  modifiedby varchar(45) DEFAULT NULL,
  modifieddate timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);
CREATE TABLE mxk_cnf_sms_provider (

  id varchar(50) NOT NULL,
  provider varchar(100) DEFAULT NULL,
  providername varchar(45) DEFAULT NULL,
  message varchar(500) DEFAULT NULL,
  appkey varchar(100) DEFAULT NULL,
  appsecret varchar(500) DEFAULT NULL,
  templateid varchar(45) DEFAULT NULL,
  signname varchar(45) DEFAULT NULL,
  smssdkappid varchar(45) DEFAULT NULL,
  status int DEFAULT NULL,
  description varchar(500) DEFAULT NULL,
  instid varchar(45) DEFAULT NULL,
  createdby varchar(45) DEFAULT NULL,
  createddate timestamp DEFAULT CURRENT_TIMESTAMP,
  modifiedby varchar(45) DEFAULT NULL,
  modifieddate timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);
CREATE TABLE mxk_connectors (

  id varchar(50) NOT NULL,
  connname varchar(200) DEFAULT NULL,
  justintime integer DEFAULT NULL,
  scheduler varchar(45) DEFAULT NULL,
  providerurl varchar(400) DEFAULT NULL,
  principal varchar(200) DEFAULT NULL,
  credentials varchar(500) DEFAULT NULL,
  filters varchar(400) DEFAULT NULL,
  status varchar(45) DEFAULT NULL,
  createdby varchar(45) DEFAULT NULL,
  createddate timestamp DEFAULT CURRENT_TIMESTAMP,
  modifiedby varchar(45) DEFAULT NULL,
  modifieddate timestamp DEFAULT NULL,
  description varchar(45) DEFAULT NULL,
  instid varchar(45) NOT NULL,
  appid varchar(45) DEFAULT NULL,
  appname varchar(45) DEFAULT NULL,
  PRIMARY KEY (id)
);
CREATE TABLE mxk_file_upload (

  id varchar(100) NOT NULL,
  filename varchar(400) DEFAULT NULL,
  uploaded bytea NOT NULL,
  contentsize int DEFAULT NULL,
  contenttype varchar(100) DEFAULT NULL,
  createdby varchar(45) DEFAULT NULL,
  createddate timestamp DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE TABLE mxk_group_member (

  id varchar(100) NOT NULL DEFAULT E'',
  groupid varchar(100) NOT NULL,
  memberid varchar(100) NOT NULL,
  type varchar(45) NOT NULL,
  createddate timestamp DEFAULT CURRENT_TIMESTAMP,
  instid varchar(45) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT mxk_group_member_groupid_memberid UNIQUE (groupid,memberid,type)
);
CREATE TABLE mxk_groups (

  id varchar(45) NOT NULL,
  groupcode varchar(45) DEFAULT NULL,
  groupname varchar(100) DEFAULT NULL,
  category varchar(20) DEFAULT NULL,
  filters text,
  orgidslist text,
  resumetime varchar(45) DEFAULT NULL,
  suspendtime varchar(45) DEFAULT NULL,
  status integer DEFAULT NULL,
  createdby varchar(45) DEFAULT NULL,
  isdefault integer DEFAULT NULL,
  createddate timestamp DEFAULT CURRENT_TIMESTAMP,
  modifiedby varchar(45) DEFAULT NULL,
  modifieddate timestamp DEFAULT NULL,
  description varchar(500) DEFAULT NULL,
  instid varchar(45) NOT NULL
);
CREATE TABLE mxk_history_connector (

  id varchar(45) NOT NULL,
  conname varchar(200) DEFAULT NULL,
  sourceid varchar(45) DEFAULT NULL,
  sourcename varchar(500) DEFAULT NULL,
  objectid varchar(45) DEFAULT NULL,
  objectname varchar(500) DEFAULT NULL,
  description varchar(1000) DEFAULT NULL,
  synctime timestamp DEFAULT CURRENT_TIMESTAMP,
  result varchar(45) DEFAULT NULL,
  instid varchar(45) NOT NULL,
  topic varchar(45) DEFAULT NULL,
  actiontype varchar(45) DEFAULT NULL,
  PRIMARY KEY (id)
);
CREATE TABLE mxk_history_event (

  id bigint GENERATED BY DEFAULT AS IDENTITY (START WITH 10) NOT NULL,
  eventname varchar(45) DEFAULT NULL,
  datatype varchar(45) DEFAULT NULL,
  datacount int DEFAULT NULL,
  executedatetime timestamp DEFAULT CURRENT_TIMESTAMP,
  instid varchar(45) NOT NULL,
  PRIMARY KEY (id)
);
CREATE TABLE mxk_history_login (

  id varchar(45) NOT NULL,
  category integer DEFAULT E'1',
  sessionid varchar(45) DEFAULT NULL,
  userid varchar(45) NOT NULL,
  username varchar(200) NOT NULL,
  displayname varchar(45) DEFAULT NULL,
  message varchar(200) DEFAULT NULL,
  sourceip varchar(300) DEFAULT NULL,
  country varchar(100) DEFAULT NULL,
  province varchar(100) DEFAULT NULL,
  city varchar(100) DEFAULT NULL,
  location varchar(500) DEFAULT NULL,
  logintype varchar(45) DEFAULT NULL,
  code varchar(45) DEFAULT NULL,
  provider varchar(45) DEFAULT NULL,
  browser varchar(45) DEFAULT NULL,
  platform varchar(45) DEFAULT NULL,
  application varchar(45) DEFAULT NULL,
  loginurl varchar(450) DEFAULT NULL,
  logintime timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  logouttime timestamp NOT NULL DEFAULT E'1970-01-01 00:00:00',
  sessionstatus int DEFAULT E'1',
  instid varchar(45) DEFAULT NULL,
  PRIMARY KEY (id)
);
CREATE TABLE mxk_history_login_apps (

  id varchar(45) NOT NULL,
  sessionid varchar(45) DEFAULT NULL,
  logintime timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  appid varchar(45) NOT NULL,
  appname varchar(45) DEFAULT NULL,
  userid varchar(45) DEFAULT NULL,
  username varchar(45) DEFAULT NULL,
  displayname varchar(45) DEFAULT NULL,
  instid varchar(45) NOT NULL,
  PRIMARY KEY (id)
);
CREATE TABLE mxk_history_provisions (

  id varchar(50) NOT NULL,
  topic varchar(45) DEFAULT NULL,
  actiontype varchar(45) DEFAULT NULL,
  content text,
  sendtime timestamp DEFAULT CURRENT_TIMESTAMP,
  connected integer DEFAULT NULL,
  instid int DEFAULT NULL,
  PRIMARY KEY (id)
);
CREATE TABLE mxk_history_synchronizer (

  id varchar(45) NOT NULL,
  syncid varchar(45) NOT NULL,
  syncname varchar(45) DEFAULT NULL,
  objectid varchar(45) DEFAULT NULL,
  objectname varchar(45) DEFAULT NULL,
  objecttype varchar(45) DEFAULT NULL,
  synctime timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  result varchar(45) DEFAULT NULL,
  instid varchar(45) NOT NULL,
  PRIMARY KEY (id)
);
CREATE TABLE mxk_history_system_logs (

  id varchar(45) NOT NULL,
  topic varchar(100) DEFAULT NULL,
  message varchar(200) DEFAULT NULL,
  messageaction varchar(45) DEFAULT NULL,
  messageresult varchar(45) DEFAULT NULL,
  userid varchar(45) DEFAULT NULL,
  username varchar(45) DEFAULT NULL,
  displayname varchar(45) DEFAULT NULL,
  executetime timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  instid varchar(45) NOT NULL,
  PRIMARY KEY (id)
);
CREATE TABLE mxk_institutions (

  id varchar(45) NOT NULL,
  name varchar(200) NOT NULL,
  fullname varchar(100) DEFAULT NULL,
  division varchar(45) DEFAULT NULL,
  country varchar(45) DEFAULT NULL,
  region varchar(45) DEFAULT NULL,
  locality varchar(45) DEFAULT NULL,
  street varchar(45) DEFAULT NULL,
  contact varchar(45) DEFAULT NULL,
  address varchar(200) DEFAULT NULL,
  postalcode varchar(45) DEFAULT NULL,
  phone varchar(200) DEFAULT NULL,
  fax varchar(200) DEFAULT NULL,
  email varchar(45) DEFAULT NULL,
  sortindex integer DEFAULT E'0',
  logo varchar(500) DEFAULT NULL,
  domain varchar(200) DEFAULT NULL,
  fronttitle varchar(200) DEFAULT NULL,
  consoledomain varchar(45) DEFAULT NULL,
  consoletitle varchar(200) DEFAULT NULL,
  captcha varchar(45) DEFAULT E'NONE,TEXT,ARITHMETIC',
  defaulturi varchar(200) DEFAULT NULL,
  status integer DEFAULT NULL,
  description varchar(200) DEFAULT NULL,
  instid varchar(45) DEFAULT NULL,
  createdby varchar(45) DEFAULT NULL,
  createddate timestamp DEFAULT CURRENT_TIMESTAMP,
  modifiedby varchar(45) DEFAULT NULL,
  modifieddate timestamp DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT mxk_institutions_domain_unique UNIQUE (domain)
);
CREATE TABLE mxk_localization (

  id varchar(45) NOT NULL,
  property varchar(200) DEFAULT NULL,
  status int DEFAULT NULL,
  description varchar(500) DEFAULT NULL,
  langzh varchar(500) DEFAULT NULL,
  langen varchar(500) DEFAULT NULL,
  instid varchar(45) DEFAULT NULL,
  PRIMARY KEY (id)
);
CREATE TABLE mxk_organizations (

  id varchar(45) NOT NULL,
  orgcode varchar(45) DEFAULT NULL,
  orgname varchar(200) NOT NULL,
  fullname varchar(100) DEFAULT NULL,
  type varchar(45) DEFAULT NULL,
  level integer DEFAULT NULL,
  parentid varchar(45) DEFAULT NULL,
  parentcode varchar(45) DEFAULT NULL,
  parentname varchar(45) DEFAULT NULL,
  codepath varchar(500) DEFAULT NULL,
  namepath varchar(400) DEFAULT NULL,
  description varchar(200) DEFAULT NULL,
  status integer DEFAULT NULL,
  createdby varchar(45) DEFAULT NULL,
  createddate timestamp DEFAULT CURRENT_TIMESTAMP,
  modifiedby varchar(45) DEFAULT NULL,
  modifieddate timestamp DEFAULT NULL,
  address varchar(200) DEFAULT NULL,
  postalcode varchar(45) DEFAULT NULL,
  phone varchar(200) DEFAULT NULL,
  fax varchar(200) DEFAULT NULL,
  sortindex integer DEFAULT E'0',
  division varchar(45) DEFAULT NULL,
  country varchar(45) DEFAULT NULL,
  region varchar(45) DEFAULT NULL,
  locality varchar(45) DEFAULT NULL,
  street varchar(45) DEFAULT NULL,
  haschild varchar(45) DEFAULT NULL,
  contact varchar(45) DEFAULT NULL,
  email varchar(45) DEFAULT NULL,
  ldapdn varchar(1000) DEFAULT NULL,
  instid varchar(45) NOT NULL,
  PRIMARY KEY (id)
);
CREATE TABLE mxk_organizations_cast (

  id varchar(45) NOT NULL,
  code varchar(45) DEFAULT NULL,
  name varchar(200) NOT NULL,
  fullname varchar(100) DEFAULT NULL,
  parentid varchar(45) DEFAULT NULL,
  parentname varchar(45) DEFAULT NULL,
  codepath varchar(500) DEFAULT NULL,
  namepath varchar(400) DEFAULT NULL,
  sortindex int DEFAULT NULL,
  status integer DEFAULT NULL,
  provider varchar(45) DEFAULT NULL,
  orgid varchar(45) DEFAULT NULL,
  orgparentid varchar(45) DEFAULT NULL,
  instid varchar(45) NOT NULL,
  createdby varchar(45) DEFAULT NULL,
  createddate timestamp DEFAULT CURRENT_TIMESTAMP,
  modifiedby varchar(45) DEFAULT NULL,
  modifieddate timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);
CREATE TABLE mxk_passkey_challenges (

  id varchar(40) NOT NULL,
  user_id varchar(40) DEFAULT NULL,
  challenge text NOT NULL,
  challenge_type varchar(20) NOT NULL,
  created_date timestamp NOT NULL,
  expires_date timestamp NOT NULL,
  status int DEFAULT E'0',
  inst_id varchar(40) DEFAULT E'1',
  PRIMARY KEY (id)
);
CREATE INDEX mxk_passkey_challenges_idx_user_id ON mxk_passkey_challenges (user_id);
CREATE INDEX mxk_passkey_challenges_idx_challenge_type ON mxk_passkey_challenges (challenge_type);
CREATE INDEX mxk_passkey_challenges_idx_expires_date ON mxk_passkey_challenges (expires_date);
CREATE INDEX mxk_passkey_challenges_idx_inst_id ON mxk_passkey_challenges (inst_id);
CREATE INDEX mxk_passkey_challenges_idx_status ON mxk_passkey_challenges (status);
CREATE INDEX mxk_passkey_challenges_idx_passkey_challenges_expires_status ON mxk_passkey_challenges (expires_date,status);
CREATE INDEX mxk_passkey_challenges_idx_passkey_challenges_user_type ON mxk_passkey_challenges (user_id,challenge_type);
CREATE TABLE mxk_permission (

  id varchar(50) NOT NULL,
  appid varchar(50) DEFAULT NULL,
  groupid varchar(50) DEFAULT NULL,
  resourceid varchar(50) DEFAULT NULL,
  createdby varchar(45) DEFAULT NULL,
  createddate timestamp DEFAULT CURRENT_TIMESTAMP,
  status int DEFAULT E'1',
  instid varchar(45) DEFAULT NULL
);
CREATE TABLE mxk_permission_role (

  id varchar(50) NOT NULL,
  appid varchar(50) DEFAULT NULL,
  roleid varchar(50) DEFAULT NULL,
  resourceid varchar(50) DEFAULT NULL,
  createdby varchar(45) DEFAULT NULL,
  createddate timestamp DEFAULT CURRENT_TIMESTAMP,
  status int DEFAULT E'1',
  instid varchar(45) DEFAULT NULL,
  PRIMARY KEY (id)
);
CREATE TABLE mxk_register (

  id varchar(50) NOT NULL,
  displayname varchar(200) DEFAULT NULL,
  workemail varchar(100) DEFAULT NULL,
  workphone varchar(50) DEFAULT NULL,
  employees int DEFAULT NULL,
  instname varchar(200) DEFAULT NULL,
  status int DEFAULT NULL,
  createdby varchar(50) DEFAULT NULL,
  createddate timestamp DEFAULT CURRENT_TIMESTAMP,
  modifiedby varchar(50) DEFAULT NULL,
  modifieddate timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);
CREATE TABLE mxk_remember_me (

  id varchar(45) NOT NULL,
  userid varchar(45) DEFAULT NULL,
  username varchar(45) DEFAULT NULL,
  lastlogintime varchar(45) DEFAULT NULL,
  expirationtime varchar(45) DEFAULT NULL,
  PRIMARY KEY (id)
);
CREATE TABLE mxk_resources (

  id varchar(50) NOT NULL,
  resourcename varchar(200) DEFAULT NULL,
  resourcetype varchar(50) DEFAULT NULL,
  resourceurl varchar(500) DEFAULT NULL,
  permission varchar(500) DEFAULT NULL,
  status varchar(45) DEFAULT NULL,
  description varchar(500) DEFAULT NULL,
  createdby varchar(45) DEFAULT NULL,
  createddate timestamp DEFAULT CURRENT_TIMESTAMP,
  modifiedby varchar(45) DEFAULT NULL,
  modifieddate timestamp DEFAULT NULL,
  parentid varchar(50) DEFAULT NULL,
  parentname varchar(200) DEFAULT NULL,
  appid varchar(50) DEFAULT NULL,
  resourceaction varchar(200) DEFAULT NULL,
  resourceicon varchar(100) DEFAULT NULL,
  resourcestyle varchar(500) DEFAULT NULL,
  sortindex int DEFAULT NULL,
  instid varchar(45) NOT NULL,
  PRIMARY KEY (id)
);
CREATE TABLE mxk_role_member (

  id varchar(100) NOT NULL DEFAULT E'',
  roleid varchar(100) NOT NULL,
  memberid varchar(100) NOT NULL,
  type varchar(45) NOT NULL,
  createddate timestamp DEFAULT CURRENT_TIMESTAMP,
  instid varchar(45) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT mxk_role_member_roleid_memberid UNIQUE (roleid,memberid,type)
);
CREATE TABLE mxk_roles (

  id varchar(45) NOT NULL,
  rolecode varchar(45) DEFAULT NULL,
  rolename varchar(100) DEFAULT NULL,
  category varchar(20) DEFAULT NULL,
  filters text,
  orgidslist text,
  resumetime varchar(45) DEFAULT NULL,
  suspendtime varchar(45) DEFAULT NULL,
  status integer DEFAULT NULL,
  isdefault integer DEFAULT NULL,
  createdby varchar(45) DEFAULT NULL,
  createddate timestamp DEFAULT CURRENT_TIMESTAMP,
  modifiedby varchar(45) DEFAULT NULL,
  modifieddate timestamp DEFAULT NULL,
  description varchar(500) DEFAULT NULL,
  instid varchar(45) NOT NULL,
  appid varchar(45) DEFAULT NULL,
  PRIMARY KEY (id)
);
CREATE TABLE mxk_socials_associate (

  id varchar(45) NOT NULL,
  userid varchar(45) NOT NULL,
  username varchar(45) NOT NULL,
  provider varchar(45) NOT NULL,
  socialuserinfo text,
  socialuserid varchar(100) NOT NULL,
  exattribute text,
  accesstoken text,
  createddate timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updateddate timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  transmission varchar(45) DEFAULT E'automatic',
  instid varchar(45) NOT NULL,
  PRIMARY KEY (id)
);
CREATE TABLE mxk_socials_provider (

  id varchar(45) NOT NULL,
  provider varchar(45) DEFAULT NULL,
  providername varchar(45) DEFAULT NULL,
  icon varchar(45) DEFAULT NULL,
  clientid varchar(100) DEFAULT NULL,
  clientsecret varchar(500) DEFAULT NULL,
  agentid varchar(45) DEFAULT NULL,
  display varchar(45) DEFAULT E'false',
  sortindex int DEFAULT E'1',
  scancode varchar(45) DEFAULT E'none',
  status int DEFAULT E'1',
  createdby varchar(45) DEFAULT NULL,
  createddate timestamp DEFAULT CURRENT_TIMESTAMP,
  modifiedby varchar(45) DEFAULT NULL,
  modifieddate timestamp DEFAULT NULL,
  instid varchar(45) NOT NULL,
  PRIMARY KEY (id)
);
CREATE TABLE mxk_synchro_association (

  id bigint GENERATED BY DEFAULT AS IDENTITY (START WITH 214) NOT NULL,
  syncid bigint NOT NULL DEFAULT E'0',
  name varchar(64) DEFAULT E'',
  objecttype varchar(10) DEFAULT E'',
  targetfield varchar(64) DEFAULT E'',
  targetfieldname varchar(64) DEFAULT E'',
  sourcefield varchar(64) DEFAULT E'',
  sourcefieldname varchar(64) DEFAULT E'',
  description varchar(200) DEFAULT E'',
  createuser bigint DEFAULT E'0',
  createtime timestamp DEFAULT NULL,
  updateuser bigint DEFAULT E'0',
  updatetime timestamp DEFAULT NULL,
  instid varchar(45) DEFAULT E'1',
  PRIMARY KEY (id)
);
CREATE INDEX mxk_synchro_association_idx_job_id ON mxk_synchro_association (syncid);
CREATE TABLE mxk_synchro_related (

  id varchar(45) NOT NULL,
  objectid varchar(45) DEFAULT NULL,
  objectname varchar(200) DEFAULT NULL,
  objectdisplayname varchar(200) DEFAULT NULL,
  objecttype varchar(45) DEFAULT NULL,
  syncid varchar(100) DEFAULT NULL,
  syncname varchar(200) DEFAULT NULL,
  originid varchar(1000) DEFAULT NULL,
  originid2 varchar(200) DEFAULT NULL,
  originid3 varchar(200) DEFAULT NULL,
  instid varchar(45) DEFAULT NULL,
  synctime timestamp DEFAULT CURRENT_TIMESTAMP,
  originname varchar(500) DEFAULT NULL,
  PRIMARY KEY (id)
);
CREATE TABLE mxk_synchronizers (

  id varchar(50) NOT NULL,
  name varchar(200) DEFAULT NULL,
  service varchar(45) DEFAULT NULL,
  sourcetype varchar(45) DEFAULT NULL,
  scheduler varchar(45) DEFAULT NULL,
  providerurl varchar(400) DEFAULT NULL,
  driverclass varchar(400) DEFAULT NULL,
  principal varchar(200) DEFAULT NULL,
  credentials varchar(500) DEFAULT NULL,
  resumetime varchar(45) DEFAULT NULL,
  suspendtime varchar(45) DEFAULT NULL,
  userbasedn varchar(200) DEFAULT NULL,
  userfilters varchar(4000) DEFAULT NULL,
  orgbasedn varchar(200) DEFAULT NULL,
  orgfilters varchar(4000) DEFAULT NULL,
  msaddomain varchar(45) DEFAULT NULL,
  sslswitch varchar(45) DEFAULT NULL,
  truststore varchar(45) DEFAULT NULL,
  truststorepassword varchar(45) DEFAULT NULL,
  syncstarttime int DEFAULT E'0',
  status varchar(45) DEFAULT NULL,
  createdby varchar(45) DEFAULT NULL,
  createddate timestamp DEFAULT CURRENT_TIMESTAMP,
  modifiedby varchar(45) DEFAULT NULL,
  modifieddate timestamp DEFAULT NULL,
  description varchar(45) DEFAULT NULL,
  instid varchar(45) NOT NULL,
  appid varchar(45) DEFAULT NULL,
  appname varchar(45) DEFAULT NULL,
  PRIMARY KEY (id)
);
CREATE TABLE mxk_user_passkeys (

  id varchar(40) NOT NULL,
  user_id varchar(40) NOT NULL,
  credential_id varchar(500) NOT NULL,
  public_key text NOT NULL,
  display_name varchar(100) DEFAULT NULL,
  device_type varchar(50) DEFAULT E'unknown',
  signature_count bigint DEFAULT E'0',
  created_date timestamp NOT NULL,
  last_used_date timestamp DEFAULT NULL,
  aaguid varchar(100) DEFAULT NULL,
  inst_id varchar(40) DEFAULT E'1',
  status int DEFAULT E'1',
  PRIMARY KEY (id),
  CONSTRAINT mxk_user_passkeys_uk_credential_id UNIQUE (credential_id)
);
CREATE INDEX mxk_user_passkeys_idx_user_id ON mxk_user_passkeys (user_id);
CREATE INDEX mxk_user_passkeys_idx_inst_id ON mxk_user_passkeys (inst_id);
CREATE INDEX mxk_user_passkeys_idx_created_date ON mxk_user_passkeys (created_date);
CREATE INDEX mxk_user_passkeys_idx_user_passkeys_user_status ON mxk_user_passkeys (user_id,status);
CREATE TABLE mxk_userinfo (

  id varchar(45) NOT NULL,
  username varchar(100) NOT NULL,
  password varchar(500) NOT NULL,
  decipherable varchar(500) NOT NULL,
  authntype integer DEFAULT E'1',
  mobile varchar(45) DEFAULT NULL,
  mobileverified varchar(45) DEFAULT NULL,
  email varchar(45) DEFAULT NULL,
  emailverified integer DEFAULT NULL,
  displayname varchar(45) DEFAULT NULL,
  nickname varchar(45) DEFAULT NULL,
  picture bytea,
  timezone varchar(45) DEFAULT E'Asia/Shanghai',
  locale varchar(45) DEFAULT E'zh_CN',
  preferredlanguage varchar(45) DEFAULT E'zh_CN',
  passwordquestion varchar(45) DEFAULT NULL,
  passwordanswer varchar(45) DEFAULT NULL,
  apploginauthntype integer DEFAULT E'0',
  apploginpassword varchar(45) DEFAULT NULL,
  protectedapps varchar(450) DEFAULT NULL,
  theme varchar(45) DEFAULT E'default',
  gridlist integer DEFAULT E'0',
  logincount integer DEFAULT E'0',
  online integer DEFAULT E'0',
  status integer DEFAULT E'1',
  islocked integer DEFAULT E'1',
  unlocktime timestamp DEFAULT E'2020-01-01 01:01:01',
  lastloginip varchar(300) DEFAULT NULL,
  lastlogintime timestamp DEFAULT E'2020-01-01 01:01:01',
  lastlogofftime timestamp DEFAULT E'2020-01-01 01:01:01',
  badpasswordtime timestamp DEFAULT E'2020-01-01 01:01:01',
  badpasswordcount integer DEFAULT NULL,
  passwordlastsettime timestamp DEFAULT E'2020-01-01 01:01:01',
  passwordsettype integer DEFAULT E'0',
  sharedsecret varchar(500) DEFAULT NULL,
  sharedcounter varchar(45) DEFAULT E'0',
  usertype varchar(45) DEFAULT E'Customer',
  userstate varchar(45) DEFAULT E'RESIDENT',
  employeenumber varchar(45) DEFAULT NULL,
  windowsaccount varchar(45) DEFAULT NULL,
  division varchar(45) DEFAULT NULL,
  costcenter varchar(45) DEFAULT NULL,
  organization varchar(45) DEFAULT NULL,
  departmentid varchar(45) DEFAULT NULL,
  department varchar(45) DEFAULT NULL,
  jobtitle varchar(45) DEFAULT NULL,
  joblevel varchar(45) DEFAULT NULL,
  managerid varchar(45) DEFAULT NULL,
  manager varchar(45) DEFAULT NULL,
  assistantid varchar(45) DEFAULT NULL,
  assistant varchar(45) DEFAULT NULL,
  entrydate varchar(45) DEFAULT NULL,
  startworkdate varchar(45) DEFAULT NULL,
  quitdate varchar(45) DEFAULT NULL,
  sortindex integer DEFAULT E'1',
  workemail varchar(45) DEFAULT NULL,
  workphonenumber varchar(45) DEFAULT NULL,
  workcountry varchar(45) DEFAULT E'CHN',
  workregion varchar(45) DEFAULT NULL,
  worklocality varchar(45) DEFAULT NULL,
  workstreetaddress varchar(45) DEFAULT NULL,
  workaddressformatted varchar(45) DEFAULT NULL,
  workpostalcode varchar(45) DEFAULT NULL,
  workfax varchar(45) DEFAULT NULL,
  workofficename varchar(500) DEFAULT NULL,
  givenname varchar(45) DEFAULT NULL,
  middlename varchar(45) DEFAULT NULL,
  familyname varchar(45) DEFAULT NULL,
  honorificprefix varchar(45) DEFAULT NULL,
  honorificsuffix varchar(45) DEFAULT NULL,
  formattedname varchar(400) DEFAULT NULL,
  idtype integer DEFAULT E'0',
  idcardno varchar(45) DEFAULT NULL,
  education varchar(200) DEFAULT NULL,
  graduatefrom varchar(500) DEFAULT NULL,
  graduatedate varchar(45) DEFAULT NULL,
  married integer DEFAULT E'0',
  birthdate varchar(45) DEFAULT NULL,
  namezhspell varchar(100) DEFAULT NULL,
  namezhshortspell varchar(45) DEFAULT NULL,
  gender integer DEFAULT NULL,
  website varchar(50) DEFAULT NULL,
  weixinfollow integer DEFAULT NULL,
  defineim varchar(45) DEFAULT NULL,
  homeemail varchar(45) DEFAULT NULL,
  homephonenumber varchar(45) DEFAULT NULL,
  homecountry varchar(45) DEFAULT E'CHN',
  homeregion varchar(45) DEFAULT NULL,
  homelocality varchar(45) DEFAULT NULL,
  homestreetaddress varchar(45) DEFAULT NULL,
  homeaddressformatted varchar(45) DEFAULT NULL,
  homepostalcode varchar(45) DEFAULT NULL,
  homefax varchar(45) DEFAULT NULL,
  extraattribute varchar(4000) DEFAULT NULL,
  createdby varchar(45) DEFAULT NULL,
  createddate timestamp DEFAULT CURRENT_TIMESTAMP,
  modifiedby varchar(45) DEFAULT NULL,
  modifieddate timestamp DEFAULT NULL,
  description varchar(400) DEFAULT NULL,
  ldapdn varchar(1000) DEFAULT NULL,
  instid varchar(45) NOT NULL,
  regionhistory text,
  passwordhistory text,
  PRIMARY KEY (id),
  CONSTRAINT mxk_userinfo_username_unique UNIQUE (username)
);
CREATE INDEX mxk_userinfo_employeenumber_unique ON mxk_userinfo (employeenumber);
