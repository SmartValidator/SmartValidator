# Smart RPKI - Validator 
======================


#### Linux OS Build status (Travis-ci) : [![Build Status](https://travis-ci.org/SmartValidator/SmartValidator.svg?branch=master)](https://travis-ci.org/SmartValidator/SmartValidator)

License
-------
This library is distributed under the BSD License.
See: https://raw.github.com/RIPE-NCC/rpki-validator/master/LICENSE.txt

Description
-----------

The Smart Validator is based on the RIPE NCC Certification Validator Tool, that allows you to validate objects that have been published in a public certificate repository.
It is designed to assist network operators in improving their BGP routing decisions using the Resource Public Key Infrastructure (RPKI) data set.

Using either preconfigured or manually added RPKI trust anchors, the Validator checks the validity of Route Origin Authorizations (ROAs).
The graphical web interface displays statistics about the RPKI validity status of retrieved BGP announcements and about ROA conflicts.
With these insights gained, an operator/you can get a good estimation of traffic loss due to RPKI application.
She/You can also – within the Validator – change parameters of the handling of ROA conflicts and directly watch the impact on her/your network traffic.

The core feature making it a Smart Validator is/will be the “what if” scenarios.
This shows the user/you what the Validator’s deployment would have changed, i.e. how many BGP attacks could have been fended off had it been deployed for a certain time, but also if and how much non-malicious traffic would have been lost.
Though actually, it is not active, so she/you can asses how valuable the Validator will be for her/your network without loosing important traffic in that process.

Installation
------------

- Prerequisite: Java 8
- The package (either downloaded pre-compiled package or own build result) contains the full
   application and necessary libraries to run the validator
- Steps to run the package are:
    ```
    tar zxf rpki-validator-app-2.24-SNAPSHOT-dist.tar.gz 
    cd rpki-validator-app-2.24-SNAPSHOT/
    ./rpki-validator.sh start
    ```
- After that the validator web interface shoud be accessible on
   http://localhost:8080
- For more details, see the "Installation" Wiki page
