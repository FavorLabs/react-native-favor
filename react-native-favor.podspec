require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "react-native-favor"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]

  s.platforms    = { :ios => "13.0" }
  s.source       = { :git => "https://github.com/FavorLabs/react-native-favor.git", :tag => "#{s.version}" }
  s.swift_version = ['5.0']

  s.source_files = "ios/**/*.{h,m,swift}"
  s.vendored_frameworks = "ios/favorX.xcframework"
  s.static_framework = true
  s.dependency "React"
  s.dependency "Starscream"
end
