#import "RongCloudChatViewController.h"
#import "RongCloudChat.h"
#import <RongIMKit/RongIMKit.h>

@implementation RongCloudChatViewController

- (void)dealloc {
  [[NSNotificationCenter defaultCenter] removeObserver:self];
  [self.customCloseButton removeFromSuperview];
}

- (UIInterfaceOrientationMask)supportedInterfaceOrientations {
  return UIInterfaceOrientationMaskPortrait;
}

- (BOOL)shouldAutorotate {
  return NO;
}

- (void)viewDidLoad {
  [super viewDidLoad];

  [[NSNotificationCenter defaultCenter] addObserver:self
                                           selector:@selector(handleApplicationWillEnterForeground)
                                               name:UIApplicationWillEnterForegroundNotification
                                             object:nil];
  
  self.displayUserNameInCell = YES;
  
  switch (self.conversationType) {
    case ConversationType_PRIVATE: {
      RCUserInfo *cachedInfo = [[RCIM sharedRCIM] getUserInfoCache:self.targetId];
      self.title = cachedInfo ? cachedInfo.name : @"";
      self.displayUserNameInCell = NO;
      break;
    }
    case ConversationType_GROUP: {
      RCGroup *group = [[RCIM sharedRCIM] getGroupInfoCache:self.targetId];
      self.title = group ? group.groupName : @"群聊";
      break;
    }
    case ConversationType_DISCUSSION: {
      self.title = @"讨论组";
      break;
    }
    default: {
      self.title = self.targetId;
      break;
    }
  }

  UIView *bottomSafeAreaView = [[UIView alloc] init];
  bottomSafeAreaView.backgroundColor = [self chatBarColor];
  bottomSafeAreaView.translatesAutoresizingMaskIntoConstraints = NO;

  [self applyNavigationAppearance];
  self.chatSessionInputBarControl.backgroundColor = [self chatBarColor];
  [self.view addSubview:bottomSafeAreaView];

  [NSLayoutConstraint activateConstraints:@[
    [bottomSafeAreaView.leadingAnchor constraintEqualToAnchor:self.view.leadingAnchor],
    [bottomSafeAreaView.trailingAnchor constraintEqualToAnchor:self.view.trailingAnchor],
    [bottomSafeAreaView.topAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.bottomAnchor],
    [bottomSafeAreaView.bottomAnchor constraintEqualToAnchor:self.view.bottomAnchor]
  ]];
}

- (void)viewWillAppear:(BOOL)animated {
  [super viewWillAppear:animated];
  [self applyNavigationAppearance];
  [self ensureCustomCloseButtonAttached];
}

- (void)viewDidLayoutSubviews {
  [super viewDidLayoutSubviews];
  [self layoutCustomCloseButton];
}

- (void)viewWillDisappear:(BOOL)animated {
  [super viewWillDisappear:animated];
  self.customCloseButton.hidden = YES;
  [RongCloudChat sendEvent:@"onRCIMChatClosed" body:@{ @"conversationType": @((NSInteger)self.conversationType), @"targetId": self.targetId ?: @"" }];
  [[RCCoreClient sharedCoreClient] getLatestMessages:(RCConversationType)self.conversationType targetId:self.targetId count:1 completion:^(NSArray<RCMessage *> * _Nullable messages) {
    if (messages.count > 0) {
      NSDictionary *data = @{
        @"conversationType": @((NSInteger)self.conversationType),
        @"targetId": self.targetId ?: @"",
        @"message": [RongCloudChat dictionaryFromRCMessage:messages.firstObject]
      };
      [RongCloudChat sendEvent:@"onRCIMChatLatestMessage" body:data];
    }
  }];
}

- (NSArray<UIMenuItem *> *)getLongTouchMessageCellMenuList:(RCMessageModel *)model {
  NSArray<UIMenuItem *> *defaultItems = [super getLongTouchMessageCellMenuList:model];
  NSMutableArray<UIMenuItem *> *filteredItems = [NSMutableArray array];

  for (UIMenuItem *item in defaultItems) {
    NSString *title = item.title ?: @"";
    if ([title containsString:@"更多"] || [title containsString:@"Select"]) {
      continue;
    }
    [filteredItems addObject:item];
  }

  return filteredItems;
}

- (void)leftBarButtonItemPressed:(id)sender {
  [super leftBarButtonItemPressed:sender];

  if (self.presentingViewController != nil) {
    [self dismissViewControllerAnimated:YES completion:nil];
    return;
  }

  [self.navigationController popViewControllerAnimated:YES];
}

- (void)handleApplicationWillEnterForeground {
  [self applyNavigationAppearance];
  [self ensureCustomCloseButtonAttached];
}

- (UIColor *)chatBarColor {
  return [UIColor colorWithRed:244/255.0 green:246/255.0 blue:249/255.0 alpha:1];
}

- (void)applyNavigationAppearance {
  UINavigationBarAppearance *appearance = [[UINavigationBarAppearance alloc] init];
  [appearance configureWithOpaqueBackground];
  appearance.backgroundColor = [self chatBarColor];
  appearance.shadowColor = [UIColor systemGray5Color];

  self.navigationItem.hidesBackButton = YES;
  self.navigationItem.leftBarButtonItem = nil;
  self.navigationItem.leftBarButtonItems = @[];
  self.navigationController.navigationBar.standardAppearance = appearance;
  self.navigationController.navigationBar.scrollEdgeAppearance = appearance;
}

- (void)ensureCustomCloseButtonAttached {
  if (self.customCloseButton == nil) {
    UIButton *button = [UIButton buttonWithType:UIButtonTypeSystem];
    [button setImage:[UIImage systemImageNamed:@"multiply"] forState:UIControlStateNormal];
    button.tintColor = [UIColor blackColor];
    button.backgroundColor = [UIColor clearColor];
    button.contentVerticalAlignment = UIControlContentVerticalAlignmentCenter;
    button.translatesAutoresizingMaskIntoConstraints = NO;
    [button addTarget:self action:@selector(leftBarButtonItemPressed:) forControlEvents:UIControlEventTouchUpInside];
    self.customCloseButton = button;
  }

  UINavigationBar *navigationBar = self.navigationController.navigationBar;
  if (navigationBar != nil && self.customCloseButton.superview != navigationBar) {
    [self.customCloseButton removeFromSuperview];
    [navigationBar addSubview:self.customCloseButton];

    [NSLayoutConstraint activateConstraints:@[
      [self.customCloseButton.leadingAnchor constraintEqualToAnchor:navigationBar.leadingAnchor constant:8.0],
      [self.customCloseButton.centerYAnchor constraintEqualToAnchor:navigationBar.centerYAnchor],
      [self.customCloseButton.widthAnchor constraintEqualToConstant:44.0],
      [self.customCloseButton.heightAnchor constraintEqualToConstant:44.0]
    ]];
  }

  self.customCloseButton.hidden = NO;
  [navigationBar bringSubviewToFront:self.customCloseButton];
}

- (void)layoutCustomCloseButton {
  if (self.customCloseButton == nil || self.customCloseButton.superview == nil) {
    return;
  }

  CGFloat topSafeAreaInset = self.view.window != nil ? self.view.window.safeAreaInsets.top : self.view.safeAreaInsets.top;
  CGFloat imageVerticalOffset = topSafeAreaInset >= 55.0 ? -4.0 : 0.0;
  self.customCloseButton.imageEdgeInsets = UIEdgeInsetsMake(imageVerticalOffset, 0, -imageVerticalOffset, 0);
  [self.customCloseButton.superview bringSubviewToFront:self.customCloseButton];
}

@end
