.PHONY: build test run stop images deploy smoke tf-plan tf-apply clean

build:            ## Build all modules
	mvn -q clean package -DskipTests

test:             ## Run all tests
	mvn -q test

run:              ## Start the full local stack (infra + services)
	docker-compose up --build

stop:             ## Stop the local stack
	docker-compose down

smoke:            ## Run the end-to-end smoke test against localhost:8080
	bash scripts/smoke-test.sh

deploy:           ## Deploy to the current kube-context (Istio required)
	bash scripts/deploy-k8s.sh

tf-plan:          ## Terraform plan (dev)
	cd terraform && terraform init && terraform plan -var-file=environments/dev.tfvars

tf-apply:         ## Terraform apply (dev)
	cd terraform && terraform apply -var-file=environments/dev.tfvars

clean:
	mvn -q clean
