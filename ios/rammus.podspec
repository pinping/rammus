#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html
#
Pod::Spec.new do |s|
  s.name             = 'rammus'
  s.version          = '0.0.1'
  s.summary          = 'A new Flutter plugin for AliCloud push.'
  s.description      = <<-DESC
A new Flutter plugin for AliCloud push.
                       DESC
  s.homepage         = 'http://example.com'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Your Company' => 'email@example.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.public_header_files = 'Classes/**/*.h'
  s.dependency 'Flutter'
  s.dependency 'AlicloudPush', '~> 1.9.9.6'   # 移动推送
  s.dependency 'AlicloudAPM', '~> 1.1.1'    # 性能分析
  s.dependency 'AlicloudMANLight', '~> 1.1.0'   # 移动数据分析
  s.dependency 'AlicloudUT', '~> 5.2.0.16'   #
  s.dependency 'AlicloudCrash', '~> 1.2.0'  # 崩溃分析

  s.frameworks = ["SystemConfiguration", "CoreTelephony"]
  s.libraries = ["z", "sqlite3.0", "resolv"]
  s.static_framework = true

  s.ios.deployment_target = '10.0'
end

