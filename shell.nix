{
  sources ? import ./nix/sources.nix,
  pkgs ? import sources.nixpkgs {},
}:

let
  gcs = pkgs.google-cloud-sdk.withExtraComponents( with pkgs.google-cloud-sdk.components; [
    beta # Not sure if needed
    kubectl # Not sure if needed
    app-engine-python # Most likely for static site plugin
  ]);
  java = pkgs.temurin-bin-17;
  gradle = pkgs.gradle_7;
  go = pkgs.go_1_22;
  python = pkgs.python312;
  node = pkgs.nodejs_22;
  protobuf = pkgs.protobuf_25;
in
  pkgs.mkShell {
    buildInputs = [
      gcs
      pkgs.lz4 # Used for tool cache
      pkgs.git
      pkgs.google-java-format
      pkgs.docker-credential-gcr
      java
      pkgs.graalvm-ce # Curio server plugin
      gradle
      python
      go

      # grpcapi plugin
      protobuf
      pkgs.protoc-gen-grpc-web

      # web plugin
      node
      pkgs.yarn

      # shell tools
      pkgs.gnused
      pkgs.gnugrep
    ];

    shellHook = ''
      export TEMP_DIR="$PWD/.temp"
      export YARN_CACHE_FOLDER="$TEMP_DIR/yarn-cache"
      export CLOUDSDK_PYTHON="${python}/bin/python"
      export CLOUDSDK_PYTHON_SITEPACKAGES=1
      export GOROOT="${go}/share/go"
      export GOPATH="$TEMP_DIR/gopath"
      export JAVA_HOME="${java}/bin"
      export PATH="$(echo -n "$PATH" | tr ":" "\n" | grep -Fv /.gradle/curiostack/ | tr "\n" ":" | sed 's/:$//')"
    '';
  }
