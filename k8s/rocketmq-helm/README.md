# RocketMQ Helm (Local)

This project installs RocketMQ via the official Helm chart as documented by Apache.

You must vendor the chart into this repo so deployments are fully reproducible from source:

```
helm pull oci://registry-1.docker.io/apache/rocketmq --version 0.0.1

tar -zxvf rocketmq-0.0.1.tgz

mkdir -p /Volumes/Work/Sporty/k8s/rocketmq-helm
mv rocketmq /Volumes/Work/Sporty/k8s/rocketmq-helm/rocketmq
```

Then edit the values file at:

```
/Volumes/Work/Sporty/k8s/rocketmq-helm/values.yaml
```

Deploy with:

```
/Volumes/Work/Sporty/k8s/deploy-local.sh
```
