#!/usr/bin/env bash

# Print commands to the terminal before execution and stop the script if any error occurs
set -ex
Kubectl delete namespace hands-on
Kubectl create namespace hands-on
Kubectl delete namespace logging
Kubectl create namespace logging

kubectl config set-context --current --namespace=hands-on

kubectl create configmap config-repo-auth-server       --from-file=config-repo/application.yml --from-file=config-repo/auth-server.yml --save-config
kubectl create configmap config-repo-gateway           --from-file=config-repo/application.yml --from-file=config-repo/gateway.yml --save-config
kubectl create configmap config-repo-product-composite --from-file=config-repo/application.yml --from-file=config-repo/product-composite.yml --save-config
kubectl create configmap config-repo-product           --from-file=config-repo/application.yml --from-file=config-repo/product.yml --save-config
kubectl create configmap config-repo-recommendation    --from-file=config-repo/application.yml --from-file=config-repo/recommendation.yml --save-config
kubectl create configmap config-repo-review            --from-file=config-repo/application.yml --from-file=config-repo/review.yml --save-config
kubectl create configmap prometheus                    --from-file=config-repo/prometheus.yml --save-config -n logging
kubectl create configmap datasource                    --from-file=config-repo/datasource.yml --save-config -n logging


kubectl create secret generic mongodb-server-credentials \
    --from-literal=MONGO_INITDB_ROOT_USERNAME=mongodb-user-dev \
    --from-literal=MONGO_INITDB_ROOT_PASSWORD=mongodb-pwd-dev \
    --save-config

kubectl create secret generic mongodb-credentials \
    --from-literal=SPRING_DATA_MONGODB_AUTHENTICATION_DATABASE=admin \
    --from-literal=SPRING_DATA_MONGODB_USERNAME=mongodb-user-dev \
    --from-literal=SPRING_DATA_MONGODB_PASSWORD=mongodb-pwd-dev \
    --save-config

kubectl create secret generic mysql-server-credentials \
    --from-literal=MYSQL_ROOT_PASSWORD=rootpwd \
    --from-literal=MYSQL_DATABASE=review-db \
    --from-literal=MYSQL_USER=mysql-user-dev \
    --from-literal=MYSQL_PASSWORD=mysql-pwd-dev \
    --save-config

kubectl create secret generic mysql-credentials \
    --from-literal=SPRING_DATASOURCE_USERNAME=mysql-user-dev \
    --from-literal=SPRING_DATASOURCE_PASSWORD=mysql-pwd-dev \
    --save-config

kubectl create secret tls tls-certificate --key kubernetes/cert/tls.key --cert kubernetes/cert/tls.crt

# First deploy the resource managers and wait for their pods to become ready
kubectl apply -f kubernetes/services/overlays/dev/zookeeper-dev.yml
kubectl apply -f kubernetes/services/overlays/dev/mongodb-dev.yml
kubectl apply -f kubernetes/services/overlays/dev/mysql-dev.yml
kubectl apply -f kubernetes/services/overlays/dev/kafka-dev.yml
kubectl wait --timeout=600s --for=condition=ready pod --all

# Next deploy the microservices and wait for their pods to become ready
kubectl apply -k kubernetes/services/overlays/dev
kubectl wait --timeout=600s --for=condition=ready pod --all


kubectl apply -f kubernetes/efk/fluentd-hands-on-configmap.yml -n kube-system
kubectl apply -f kubernetes/efk/fluentd-ds.yml -n kube-system

kubectl apply -f kubernetes/efk/elasticsearch.yml -n logging
kubectl apply -f kubernetes/efk/kibana.yml -n logging
kubectl apply -f kubernetes/pg/prometheus.yml -n logging
kubectl apply -f kubernetes/pg/grafana.yml -n logging

set +ex
