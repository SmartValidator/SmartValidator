#!/usr/bin/env bash
mkdir ./rpki-validator-app/target/rpki-validator-app
tar -xvf ./rpki-validator-app/target/rpki-validator-app-2.24-SNAPSHOT-dist.tar.gz -C ./rpki-validator-app/target/rpki-validator-app
./rpki-validator-app/target/rpki-validator-app/rpki-validator-app-2.24-SNAPSHOT/rpki-validator.sh start