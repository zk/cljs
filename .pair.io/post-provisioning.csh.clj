(sudo
 (apt-install :openjdk-6-jdk))

(sudo
 (wget "https://github.com/technomancy/leiningen/raw/stable/bin/lein"
       "--no-check-certificate")
 (chmod :0755 "./lein")
 (mv "./lein" "/usr/bin"))

($ "lein")
