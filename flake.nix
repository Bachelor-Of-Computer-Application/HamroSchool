{
  description = "Hamro School Application - Development Environment";

  inputs.nixpkgs.url = "https://flakehub.com/f/NixOS/nixpkgs/0.1";

  outputs = { self, ... }@inputs:

  let
    javaVersion = 21;

    supportedSystems = [
      "x86_64-linux"
      "aarch64-linux"
      "x86_64-darwin"
      "aarch64-darwin"
    ];

    forEachSupportedSystem = f:
      inputs.nixpkgs.lib.genAttrs supportedSystems (system:
        f {
          pkgs = import inputs.nixpkgs {
            inherit system;
            overlays = [ self.overlays.default ];
          };
        }
      );

  in
  {
    overlays.default = final: prev:
      let
        jdk = prev."jdk${toString javaVersion}";
      in
      {
        inherit jdk;
        maven = prev.maven.override { jdk_headless = jdk; };
      };

    devShells = forEachSupportedSystem ({ pkgs }: {
      default = pkgs.mkShell {
        name = "school-management-system-dev";

        packages = with pkgs; [

          # =====================
          # Build tools
          # =====================
          jdk
          maven
          git
          curl
          wget
          gcc
          gnumake
          jdt-language-server

          # =====================
          # JavaFX runtime deps
          # =====================

          # X11 / OpenGL (fixes libXxf86vm + rendering issues)
          xorg.libXxf86vm
          xorg.libXtst
          xorg.libXi
          xorg.libXrandr
          xorg.libXrender
          xorg.libXcursor
          xorg.libXext

          # GTK backend (fixes glassgtk3 + gthread)
          glib
          gtk3
          gdk-pixbuf
          pango
          cairo
          atk

          # graphics stack
          mesa
          fontconfig
          freetype
        ];

        env = {
          JAVA_HOME = "${pkgs.jdk}";
          MAVEN_OPTS = "-Xmx2048m";
          PROJECT_DIR = builtins.toString ./.;
          DATABASE_FILE = "data/hamroschool";

          # IMPORTANT: native libs for JavaFX
          LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath [
            pkgs.xorg.libXxf86vm
            pkgs.xorg.libXtst
            pkgs.xorg.libXi
            pkgs.xorg.libXrandr
            pkgs.xorg.libXrender
            pkgs.xorg.libXcursor
            pkgs.xorg.libXext
            pkgs.glib
            pkgs.gtk3
            pkgs.mesa
          ];
        };
      };
    });

    apps = forEachSupportedSystem ({ pkgs }: {
      build = {
        type = "app";
        program = toString (pkgs.writeShellScript "build" ''
          cd ${builtins.toString ./.}
          ${pkgs.maven}/bin/mvn clean install
        '');
      };

      run = {
        type = "app";
        program = toString (pkgs.writeShellScript "run" ''
          cd ${builtins.toString ./.}
          ${pkgs.maven}/bin/mvn clean javafx:run
        '');
      };

      package = {
        type = "app";
        program = toString (pkgs.writeShellScript "package" ''
          cd ${builtins.toString ./.}
          ${pkgs.maven}/bin/mvn clean package
        '');
      };
    });
  };
}
