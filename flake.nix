{
  description = "Hammro School Application - Development Environment";

  inputs.nixpkgs.url = "https://flakehub.com/f/NixOS/nixpkgs/0.1"; # unstable Nixpkgs

  outputs =
    { self, ... }@inputs:

    let
      javaVersion = 11; # Java 11 LTS for School Management System

      supportedSystems = [
        "x86_64-linux"
        "aarch64-linux"
        "x86_64-darwin"
        "aarch64-darwin"
      ];
      forEachSupportedSystem =
        f:
        inputs.nixpkgs.lib.genAttrs supportedSystems (
          system:
          f {
            pkgs = import inputs.nixpkgs {
              inherit system;
              overlays = [ inputs.self.overlays.default ];
            };
          }
        );
    in
    {
      overlays.default =
        final: prev:
        let
          jdk = prev."jdk${toString javaVersion}";
        in
        {
          inherit jdk;
          maven = prev.maven.override { jdk_headless = jdk; };
          gradle = prev.gradle.override { java = jdk; };
          lombok = prev.lombok.override { inherit jdk; };
        };

      devShells = forEachSupportedSystem (
        { pkgs }:
        {
          default = pkgs.mkShell {
            name = "school-management-system-dev";
            
            packages = with pkgs; [
              # Java build tools
              jdk
              maven
              gradle
              
              # Database
              sqlite
              sqlitebrowser  # GUI for viewing SQLite databases
              
              # Development utilities
              git
              git-lfs
              curl
              wget
              
              # Code quality
              gcc
              gnumake
              
              # Documentation
              pandoc
              
              # Debugging & profiling
              jdt-language-server
              
              # Additional utilities
              vim
              nano
              htop
            ];

            env = {
              # Ensure we use the correct Java version
              JAVA_HOME = "${pkgs.jdk}";
              
              # Maven configuration
              MAVEN_OPTS = "-Xmx2048m";
              
              # Project-specific environment
              PROJECT_DIR = builtins.toString ./.;
              DATABASE_FILE = "school_management.db";
            };

            shellHook = ''
              echo "╔════════════════════════════════════════════════════════════╗"
              echo "║    Welcome to School Management System Development Shell   ║"
              echo "╚════════════════════════════════════════════════════════════╝"
              echo ""
              echo "📦 Installed Tools:"
              echo "   Java: $(java -version 2>&1 | head -n 1)"
              echo "   Maven: $(mvn --version | head -n 1)"
              echo "   SQLite: $(sqlite3 --version)"
              echo ""
              echo "🚀 Quick Commands:"
              echo "   Build:  mvn clean install"
              echo "   Run:    mvn exec:java -Dexec.mainClass=\"com.schoolms.Main\""
              echo "   Package: mvn clean package"
              echo "   Test:   mvn test"
              echo "   DB:     sqlite3 school_management.db"
              echo ""
              echo "📚 Documentation:"
              echo "   cat README.md         - Full documentation"
              echo "   cat QUICKSTART.md     - Quick start guide"
              echo "   cat DEVELOPMENT.md    - Development guide"
              echo ""
              echo "Type 'exit' to leave the development shell"
              echo ""
            '';
          };
        }
      );

      # Flake apps for convenient project execution
      apps = forEachSupportedSystem (
        { pkgs }:
        {
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
              ${pkgs.maven}/bin/mvn exec:java -Dexec.mainClass="com.schoolms.Main"
            '');
          };

          package = {
            type = "app";
            program = toString (pkgs.writeShellScript "package" ''
              cd ${builtins.toString ./.}
              ${pkgs.maven}/bin/mvn clean package
            '');
          };

          test = {
            type = "app";
            program = toString (pkgs.writeShellScript "test" ''
              cd ${builtins.toString ./.}
              ${pkgs.maven}/bin/mvn test
            '');
          };

          db = {
            type = "app";
            program = toString (pkgs.writeShellScript "db" ''
              ${pkgs.sqlite}/bin/sqlite3 ${builtins.toString ./school_management.db}
            '');
          };

          db-browser = {
            type = "app";
            program = toString (pkgs.writeShellScript "db-browser" ''
              ${pkgs.sqlitebrowser}/bin/sqlitebrowser ${builtins.toString ./school_management.db}
            '');
          };
        }
      );
    };
}
