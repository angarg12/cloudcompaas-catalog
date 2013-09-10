	CREATE TABLE SLA_TEMPLATE (
	       id_sla_template INT NOT NULL IDENTITY
	     , template VARCHAR(32768)
	)
	
	CREATE TABLE SLA (
	       id_sla INT NOT NULL IDENTITY
	     , state VARCHAR(32)
	     , xmlsla VARCHAR(32768)
	)
	
	CREATE TABLE DOMAIN (
	       id_domain INT NOT NULL IDENTITY
	     , name VARCHAR(32)
	     , lft INT
	     , rght INT
	)
	
	CREATE TABLE VIRTUALMACHINE (
	       id_virtualmachine INT NOT NULL IDENTITY
	     , name VARCHAR(32)
	     , id_sla_template INT
	     , CONSTRAINT FK_VM_1 FOREIGN KEY (id_sla_template)
	                  REFERENCES SLA_TEMPLATE (id_sla_template)
	)
	
	CREATE TABLE SOFTRESOURCE (
	       id_softresource INT NOT NULL IDENTITY
	     , name VARCHAR(32)
	     , id_sla_template INT
	     , CONSTRAINT FK_SOFT_RESOURCES_1 FOREIGN KEY (id_sla_template)
	                  REFERENCES SLA_TEMPLATE (id_sla_template)
	)
	
	CREATE TABLE VIRTUALRUNTIME (
	       id_virtualruntime INT NOT NULL IDENTITY
	     , name VARCHAR(32)
	     , id_sla_template INT
	     , CONSTRAINT FK_VR_2 FOREIGN KEY (id_sla_template)
	                  REFERENCES SLA_TEMPLATE (id_sla_template)
	)
	
	CREATE TABLE VIRTUALCONTAINER (
	       id_virtualcontainer INT NOT NULL IDENTITY
	     , name VARCHAR(32)
	     , id_sla_template INT
	     , CONSTRAINT FK_VC_1 FOREIGN KEY (id_sla_template)
	                  REFERENCES SLA_TEMPLATE (id_sla_template)
	)
	
	CREATE TABLE USER (
	       id_user INT NOT NULL IDENTITY
	     , name VARCHAR(128)
	     , reputation INT
	     , credits INT
	     , current_credits INT
	     , passwd VARCHAR(512)
	     , id_sla_template INT
	     , CONSTRAINT FK_USER_1 FOREIGN KEY (id_sla_template)
	                  REFERENCES SLA_TEMPLATE (id_sla_template)
	)
	
	CREATE TABLE ORGANIZATION (
	       id_organization INT NOT NULL IDENTITY
	     , name VARCHAR(128)
	     , reputation INT
	     , credits INT
	     , current_credits INT
	     , id_sla_template INT
	     , CONSTRAINT FK_VO_1 FOREIGN KEY (id_sla_template)
	                  REFERENCES SLA_TEMPLATE (id_sla_template)
	)
	
	CREATE TABLE SERVICE (
	       id_service INT NOT NULL IDENTITY
	     , name VARCHAR(64)
	     , desc VARCHAR(1024)
	     , epr VARCHAR(64)
	     , id_sla_template INT
	     , CONSTRAINT FK_SERVICE_1 FOREIGN KEY (id_sla_template)
	                  REFERENCES SLA_TEMPLATE (id_sla_template)
	)
	
	CREATE TABLE PERMISSION (
	       id_permission INT NOT NULL IDENTITY
	     , target INT
	     , action VARCHAR(32)
	     , source VARCHAR(64)
	     , destiny VARCHAR(64)
	     , allowed BOOLEAN
	     , CONSTRAINT FK_PERMISSIONS_1 FOREIGN KEY (target)
	                  REFERENCES DOMAIN (id_domain)
	)
	
	CREATE TABLE SOFTADDON (
	       id_softaddon INT NOT NULL IDENTITY
	     , name VARCHAR(32)
	     , id_sla_template INT
	     , CONSTRAINT FK_SOFT_ADD_ON_1 FOREIGN KEY (id_sla_template)
	                  REFERENCES SLA_TEMPLATE (id_sla_template)
	)
	
	CREATE TABLE VM_INSTANCE (
	       id_vm_instance INT NOT NULL IDENTITY
	     , epr VARCHAR(128)
	     , id_sla INT
	     , id_vm_instance_local_iaasagent INT
	     , local_sdt_id VARCHAR(64)
	     , id_iaasagent INT NOT NULL
	     , CONSTRAINT FK_VM_INSTANCE_1_2 FOREIGN KEY (id_sla)
	                  REFERENCES SLA (id_sla)
	)
	
	CREATE TABLE PHYSICALRESOURCE (
	       id_physicalresource INT NOT NULL IDENTITY
	     , name VARCHAR(32)
	     , value VARCHAR(32)
	     , id_sla_template INT
	     , CONSTRAINT FK_RESOURCE_1 FOREIGN KEY (id_sla_template)
	                  REFERENCES SLA_TEMPLATE (id_sla_template)
	)
	
	CREATE TABLE VR_INSTANCE (
	       id_vr_instance INT NOT NULL IDENTITY
	     , epr VARCHAR(128)
	     , id_vm_instance INT
	     , id_sla INT
	     , local_sdt_id VARCHAR(64)
	     , CONSTRAINT FK_VR_INSTANCE_1_1 FOREIGN KEY (id_vm_instance)
	                  REFERENCES VM_INSTANCE (id_vm_instance)
	     , CONSTRAINT FK_VR_INSTANCE_1_2 FOREIGN KEY (id_sla)
	                  REFERENCES SLA (id_sla)
	)
	
	CREATE TABLE HAS_SOFTRESOURCE (
	       id_virtualruntime INT NOT NULL
	     , id_softresource INT NOT NULL
	     , PRIMARY KEY (id_virtualruntime, id_softresource)
	     , CONSTRAINT FK_HAS_SOFT_RESOURCE_1 FOREIGN KEY (id_softresource)
	                  REFERENCES SOFTRESOURCE (id_softresource)
	     , CONSTRAINT FK_HAS_SOFT_RESOURCE_2 FOREIGN KEY (id_virtualruntime)
	                  REFERENCES VIRTUALRUNTIME (id_virtualruntime)
	)
	
	CREATE TABLE OPERATINGSYSTEM (
	       id_operatingsystem INT NOT NULL IDENTITY
	     , id BIGINT
	     , name VARCHAR(32)
	     , version VARCHAR(32)
	     , flavour VARCHAR(32)
	     , hypervisor VARCHAR(32)
	     , id_sla_template INT
	     , CONSTRAINT FK_OS_1 FOREIGN KEY (id_sla_template)
	                  REFERENCES SLA_TEMPLATE (id_sla_template)
	)
	
	CREATE TABLE HAS_VIRTUALRUNTIME (
	       id_virtualcontainer INT NOT NULL
	     , id_virtualruntime INT NOT NULL
	     , PRIMARY KEY (id_virtualcontainer, id_virtualruntime)
	     , CONSTRAINT FK_HAS_VR_2 FOREIGN KEY (id_virtualcontainer)
	                  REFERENCES VIRTUALCONTAINER (id_virtualcontainer)
	     , CONSTRAINT FK_HAS_VR_1 FOREIGN KEY (id_virtualruntime)
	                  REFERENCES VIRTUALRUNTIME (id_virtualruntime)
	)
	
	CREATE TABLE HAS_USER (
	       id_user INT NOT NULL
	     , id_organization INT NOT NULL
	     , PRIMARY KEY (id_user, id_organization)
	     , CONSTRAINT FK_HAS_USER_1 FOREIGN KEY (id_organization)
	                  REFERENCES ORGANIZATION (id_organization)
	     , CONSTRAINT FK_HAS_USER_2 FOREIGN KEY (id_user)
	                  REFERENCES USER (id_user)
	)
	
	CREATE TABLE HAS_DOMAIN (
	       id_domain INT NOT NULL
	     , id_sla_template INT NOT NULL
	     , PRIMARY KEY (id_domain, id_sla_template)
	     , CONSTRAINT FK_HAS_DOMAIN_1 FOREIGN KEY (id_domain)
	                  REFERENCES DOMAIN (id_domain)
	     , CONSTRAINT FK_HAS_DOMAIN_2 FOREIGN KEY (id_sla_template)
	                  REFERENCES SLA_TEMPLATE (id_sla_template)
	)
	
	CREATE TABLE SERVICEVERSION (
	       id_serviceversion INT NOT NULL IDENTITY
	     , version VARCHAR(32)
	     , id_service INT
	     , id_sla_template INT
	     , CONSTRAINT FK_VERSION_1 FOREIGN KEY (id_service)
	                  REFERENCES SERVICE (id_service)
	     , CONSTRAINT FK_VERSION_2 FOREIGN KEY (id_sla_template)
	                  REFERENCES SLA_TEMPLATE (id_sla_template)
	)
	
	CREATE TABLE HAS_SLA_TEMPLATE (
	       id_sla INT NOT NULL
	     , id_sla_template INT NOT NULL
	     , PRIMARY KEY (id_sla, id_sla_template)
	     , CONSTRAINT FK_HAS_TEMPLATE_1 FOREIGN KEY (id_sla_template)
	                  REFERENCES SLA_TEMPLATE (id_sla_template)
	     , CONSTRAINT FK_HAS_TEMPLATE_2 FOREIGN KEY (id_sla)
	                  REFERENCES SLA (id_sla)
	)
	
	CREATE TABLE HAS_PERMISSION (
	       id_permission INT NOT NULL
	     , id_sla_template INT NOT NULL
	     , PRIMARY KEY (id_permission, id_sla_template)
	     , CONSTRAINT FK_HAS_PERMISSIONS_1 FOREIGN KEY (id_sla_template)
	                  REFERENCES SLA_TEMPLATE (id_sla_template)
	     , CONSTRAINT FK_HAS_PERMISSIONS_2 FOREIGN KEY (id_permission)
	                  REFERENCES PERMISSION (id_permission)
	)
	
	CREATE TABLE HAS_SOFTADDON (
	       id_softaddon INT NOT NULL
	     , id_softresource INT NOT NULL
	     , PRIMARY KEY (id_softaddon, id_softresource)
	     , CONSTRAINT FK_HAS_ADD_ON_1 FOREIGN KEY (id_softaddon)
	                  REFERENCES SOFTADDON (id_softaddon)
	     , CONSTRAINT FK_HAS_ADD_ON_2 FOREIGN KEY (id_softresource)
	                  REFERENCES SOFTRESOURCE (id_softresource)
	)
	
	CREATE TABLE GUARANTEE (
	       id_guarantee INT NOT NULL IDENTITY
	     , name VARCHAR(32)
	     , id_sla_template INT
	     , CONSTRAINT FK_GUARANTEE_1 FOREIGN KEY (id_sla_template)
	                  REFERENCES SLA_TEMPLATE (id_sla_template)
	)
	
	CREATE TABLE MONITORING_INFORMATION (
	       id_monitoring_information INT NOT NULL IDENTITY
	     , epr VARCHAR(128)
	     , metric_name VARCHAR(32)
	     , metric_value DOUBLE
	     , timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
	     , id_sla INT
	     , local_sdt_id VARCHAR(64)
	     , CONSTRAINT FK_MONITORING_INFORMATION_1_1 FOREIGN KEY (id_sla)
	                  REFERENCES SLA (id_sla)
	)
	
	CREATE TABLE GUARANTEE_TERMS (
	       id_guarantee_terms INT NOT NULL IDENTITY
	     , local_guarantee_id VARCHAR(64)
	     , state VARCHAR(8192)
	     , id_sla INT
	     , CONSTRAINT FK_GUARANTEE_TERMS_1_1 FOREIGN KEY (id_sla)
	                  REFERENCES SLA (id_sla)
	)
	
	CREATE TABLE SERVICE_DESCRIPTION_TERMS (
	       id_service_description_terms INT NOT NULL IDENTITY
	     , local_sdt_id VARCHAR(64)
	     , state VARCHAR(8192)
	     , id_sla INT
	     , CONSTRAINT FK_SERVICE_DESCRIPTION_TERMS_1_1 FOREIGN KEY (id_sla)
	                  REFERENCES SLA (id_sla)
	)
	
	CREATE TABLE SERVICE_INSTANCE (
	       id_service_instance INT NOT NULL IDENTITY
	     , epr VARCHAR(128)
	     , service INT
	     , version INT
	     , id_vr_instance INT
	     , id_sla INT
	     , local_sdt_id VARCHAR(64)
	     , CONSTRAINT FK_SERVICE_INSTANCE_1_1 FOREIGN KEY (id_vr_instance)
	                  REFERENCES VR_INSTANCE (id_vr_instance)
	     , CONSTRAINT FK_SERVICE_INSTANCE_1_2 FOREIGN KEY (id_sla)
	                  REFERENCES SLA (id_sla)
	)
	
	CREATE TABLE HAS_PHYSICALRESOURCE (
	       id_virtualmachine INT NOT NULL
	     , id_physicalresource INT NOT NULL
	     , PRIMARY KEY (id_virtualmachine, id_physicalresource)
	     , CONSTRAINT FK_HAS_RESOURCE_1 FOREIGN KEY (id_physicalresource)
	                  REFERENCES PHYSICALRESOURCE (id_physicalresource)
	     , CONSTRAINT FK_HAS_RESOURCE_2 FOREIGN KEY (id_virtualmachine)
	                  REFERENCES VIRTUALMACHINE (id_virtualmachine)
	);

